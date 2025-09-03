package tech.powerscheduler.server.application.actor.singleton

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.RetentionPolicyEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.domainevent.DomainEventRepository
import tech.powerscheduler.server.domain.domainevent.DomainEventStatusEnum
import tech.powerscheduler.server.domain.job.JobInfo
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.domain.job.JobInstanceId
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.workflow.Workflow
import tech.powerscheduler.server.domain.workflow.WorkflowInstance
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * @author grayrat
 * @since 2025/6/4
 */
class SystemCleanupActor(
    context: ActorContext<Command>,
    private val taskRepository: TaskRepository,
    private val jobInfoRepository: JobInfoRepository,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workflowRepository: WorkflowRepository,
    private val domainEventRepository: DomainEventRepository,
    private val workflowInstanceRepository: WorkflowInstanceRepository,
    private val transactionTemplate: TransactionTemplate,
) : AbstractBehavior<SystemCleanupActor.Command>(context) {

    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface Command {
        object CleanUp : Command
    }

    companion object {
        fun create(applicationContext: ApplicationContext): Behavior<Command> {
            val jobInfoRepository = applicationContext.getBean(JobInfoRepository::class.java)
            val jobInstanceRepository = applicationContext.getBean(JobInstanceRepository::class.java)
            val taskRepository = applicationContext.getBean(TaskRepository::class.java)
            val domainEventRepository = applicationContext.getBean(DomainEventRepository::class.java)
            val workflowRepository = applicationContext.getBean(WorkflowRepository::class.java)
            val workflowInstanceRepository = applicationContext.getBean(WorkflowInstanceRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = SystemCleanupActor(
                        context = context,
                        jobInfoRepository = jobInfoRepository,
                        jobInstanceRepository = jobInstanceRepository,
                        taskRepository = taskRepository,
                        domainEventRepository = domainEventRepository,
                        workflowRepository = workflowRepository,
                        workflowInstanceRepository = workflowInstanceRepository,
                        transactionTemplate = transactionTemplate,
                    )
                    timer.startTimerWithFixedDelay(
                        Command.CleanUp,
                        Command.CleanUp,
                        Duration.ofSeconds(300),
                        Duration.ofHours(12),
                    )
                    return@withTimers actor
                }
            }.apply {
                Behaviors.supervise(this).onFailure(SupervisorStrategy.resume())
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessageEquals(Command.CleanUp, this::handleCleanUp)
            .build()
    }

    fun handleCleanUp(): Behavior<Command> {
        cleanUpByJobInfo()
        cleanUpByWorkflow()
        cleanDomainEvent()
        return this
    }

    private fun cleanUpByJobInfo() {
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 50)
            val page = jobInfoRepository.listAllIds(query)
            val jobIds = page.content
            val jobInfos = jobInfoRepository.findAllByIds(jobIds)
            cleanJobInstance(jobInfos)
        } while (page.isNotEmpty())
        log.info("clean jobJobInstance successfully")
    }

    private fun cleanUpByWorkflow() {
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 50)
            val page = workflowRepository.listAllIds(query)
            val workflowIds = page.content
            val workflows = workflowRepository.findAllByIds(workflowIds)
            cleanWorkflowInstance(workflows)
        } while (page.isNotEmpty())
        log.info("clean workflowInstance successfully")
    }

    fun cleanJobInstance(jobInfos: Iterable<JobInfo>) {
        val retentionPolicy2JobInfos = jobInfos
            .filter { it.retentionPolicy != null }
            .groupBy { it.retentionPolicy!! }
        cleanJobInstanceByRetainRecentCountPolicy(retentionPolicy2JobInfos[RetentionPolicyEnum.RECENT_COUNT].orEmpty())
        cleanJobInstanceByRecentDayPolicy(retentionPolicy2JobInfos[RetentionPolicyEnum.RECENT_DAYS].orEmpty())
    }

    private fun cleanWorkflowInstance(workflows: Iterable<Workflow>) {
        val retentionPolicy2Workflows = workflows
            .filter { it.retentionPolicy != null }
            .groupBy { it.retentionPolicy!! }
        cleanWorkflowInstanceByRetainRecentCountPolicy(retentionPolicy2Workflows[RetentionPolicyEnum.RECENT_COUNT].orEmpty())
        cleanWorkflowInstanceByRecentDayPolicy(retentionPolicy2Workflows[RetentionPolicyEnum.RECENT_DAYS].orEmpty())
    }

    private fun cleanJobInstanceByRetainRecentCountPolicy(jobInfos: Iterable<JobInfo>) {
        val jobId2JobInfo = jobInfos.associateBy { it.id }
        val jobIdsRetainedByCount = jobInfos.mapNotNull { it.id }
        val jobId2Count = jobInstanceRepository.countByJobIdAndJobStatus(
            jobIds = jobIdsRetainedByCount,
            jobStatuses = JobStatusEnum.COMPLETED_STATUSES
        )
        val needCleanJobIds = jobInfos.asSequence()
            .filter { it.retentionValue!! > 0 }
            .filter { (jobId2Count[it.id!!] ?: 0) > it.retentionValue!! }
            .mapNotNull { it.id }
            .toList()

        for (jobId in needCleanJobIds) {
            val count = jobId2Count[jobId]!!
            val jobInfo = jobId2JobInfo[jobId]!!

            val retainCount = jobInfo.retentionValue ?: 300
            var remaining = count - retainCount
            while (remaining > 0) {
                val cleanCnt = doCleanUpJobInstance {
                    jobInstanceRepository.listIdByJobIdAndJobStatus(
                        jobId = jobId,
                        jobStatuses = JobStatusEnum.COMPLETED_STATUSES,
                        pageQuery = PageQuery(pageNo = 1, pageSize = 200),
                    )
                }
                remaining -= cleanCnt
                if (cleanCnt == 0 || remaining == 0L) {
                    break
                }
            }
            log.info("clean [{}] jobInstance, job=[{}], retainCount={}", count, jobId.value, jobInfo.retentionValue)
        }
    }

    private fun cleanJobInstanceByRecentDayPolicy(jobInfos: Iterable<JobInfo>) {
        val now = LocalDateTime.now()
        for (jobInfo in jobInfos) {
            val retentionDays = jobInfo.retentionValue ?: 15
            val jobId = jobInfo.id!!
            val expireTime = now.minusDays(retentionDays.toLong()).truncatedTo(ChronoUnit.DAYS)

            var totalCount = 0
            while (true) {
                val cleanCnt = doCleanUpJobInstance {
                    jobInstanceRepository.listIdByJobIdAndJobStatusAndEndAtBefore(
                        jobId = jobId,
                        jobStatuses = JobStatusEnum.COMPLETED_STATUSES,
                        endAt = expireTime,
                        pageQuery = PageQuery(pageNo = 1, pageSize = 200),
                    )
                }
                totalCount += cleanCnt
                if (cleanCnt == 0) {
                    break
                }
            }
            log.info("clean [{}] jobInstance, jobId=[{}], expireTime={}", totalCount, jobId.value, expireTime)
        }
    }

    private fun cleanWorkflowInstanceByRetainRecentCountPolicy(workflows: Iterable<Workflow>) {
        val id2Workflow = workflows.associateBy { it.id }
        val workflowIds = workflows.mapNotNull { it.id }
        val workflowId2Count = workflowInstanceRepository.countByWorkflowIdAndStatus(
            workflowIds = workflowIds,
            statuses = WorkflowStatusEnum.COMPLETED_STATUSES,
        )
        val workflowIdsToCleanUp = workflows.asSequence()
            .filter { it.retentionValue!! > 0 }
            .filter { (workflowId2Count[it.id!!] ?: 0) > it.retentionValue!! }
            .mapNotNull { it.id }
            .toList()

        for (workflowId in workflowIdsToCleanUp) {
            val count = workflowId2Count[workflowId]!!
            val workflow = id2Workflow[workflowId]!!

            val retainCount = workflow.retentionValue ?: 300
            var remaining = count - retainCount
            while (remaining > 0) {
                val cleanCnt = doCleanUpWorkflow {
                    workflowInstanceRepository.findAllByWorkflowIdAndStatus(
                        workflowId = workflowId,
                        statuses = WorkflowStatusEnum.COMPLETED_STATUSES,
                        pageQuery = PageQuery(pageNo = 1, pageSize = 20),
                    )
                }
                remaining -= cleanCnt
                if (cleanCnt == 0 || remaining == 0L) {
                    break
                }
            }
            log.info(
                "clean [{}] workflowInstanceInstance, workflowId=[{}], retainCount={}",
                count,
                workflowId.value,
                workflow.retentionValue
            )
        }
    }

    private fun cleanWorkflowInstanceByRecentDayPolicy(workflows: List<Workflow>) {
        val now = LocalDateTime.now()

        for (workflow in workflows) {
            val retentionDays = workflow.retentionValue ?: 15
            val workflowId = workflow.id!!
            val expireTime = now.minusDays(retentionDays.toLong()).truncatedTo(ChronoUnit.DAYS)

            var totalCount = 0
            while (true) {
                val cleanCnt = doCleanUpWorkflow {
                    workflowInstanceRepository.findAllByWorkflowIdAndStatusAndEndAtBefore(
                        workflowId = workflowId,
                        statuses = WorkflowStatusEnum.COMPLETED_STATUSES,
                        endAt = expireTime,
                        pageQuery = PageQuery(pageNo = 1, pageSize = 20),
                    )
                }
                totalCount += cleanCnt
                if (cleanCnt == 0) {
                    break
                }
            }
            log.info(
                "clean [{}] workflowInstance, workflowId=[{}], expireTime={}",
                totalCount,
                workflowId.value,
                expireTime
            )
        }
    }

    private fun doCleanUpJobInstance(dataProvider: () -> Page<JobInstanceId>): Int {
        return transactionTemplate.execute {
            val page = dataProvider()
            if (page.isEmpty()) {
                return@execute 0
            }
            val jobInstanceIds = page.content
            transactionTemplate.executeWithoutResult {
                taskRepository.deleteByJobInstanceId(jobInstanceIds)
                jobInstanceRepository.deleteByIds(jobInstanceIds)
            }
            return@execute jobInstanceIds.size
        }!!
    }

    private fun doCleanUpWorkflow(
        dataProvider: () -> Page<WorkflowInstance>
    ): Int {
        return transactionTemplate.execute {
            val page = dataProvider()
            if (page.isEmpty()) {
                return@execute 0
            }
            val workflowInstances = page.content
            val workflowInstanceCodes = workflowInstances.mapNotNull { it.code }
            val jobInstanceIds = jobInstanceRepository.listIdByWorkflowInstanceCodes(workflowInstanceCodes)
            taskRepository.deleteByJobInstanceId(jobInstanceIds)
            jobInstanceRepository.deleteByIds(jobInstanceIds)
            workflowInstanceRepository.deleteAll(workflowInstances)
            return@execute workflowInstances.size
        }!!
    }

    private fun cleanDomainEvent() {
        domainEventRepository.deleteByEventStatus(DomainEventStatusEnum.SUCCESS)
    }
}