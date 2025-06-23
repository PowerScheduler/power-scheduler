package tech.powerscheduler.server.application.actor

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.ScheduleTypeEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.appgroup.AppGroupKey
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.worker.WorkerRegistry
import tech.powerscheduler.server.domain.worker.WorkerRegistryRepository
import tech.powerscheduler.server.domain.workflow.WorkflowId
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowNodeRepository
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
import java.time.Duration
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/25
 */
class WorkflowSchedulerActor(
    context: ActorContext<Command>,
    private val serverAddressHolder: ServerAddressHolder,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workflowRepository: WorkflowRepository,
    private val workflowNodeRepository: WorkflowNodeRepository,
    private val workflowInstanceRepository: WorkflowInstanceRepository,
    private val workerRegistryRepository: WorkerRegistryRepository,
    private val taskRepository: TaskRepository,
    private val transactionTemplate: TransactionTemplate,
) : AbstractBehavior<WorkflowSchedulerActor.Command>(context) {

    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface Command {
        object ScheduleWorkflows : Command
        object CreateTasks : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val serverAddressHolder = applicationContext.getBean(ServerAddressHolder::class.java)
            val jobInstanceRepository = applicationContext.getBean(JobInstanceRepository::class.java)
            val workflowRepository = applicationContext.getBean(WorkflowRepository::class.java)
            val workflowNodeRepository = applicationContext.getBean(WorkflowNodeRepository::class.java)
            val workflowInstanceRepository = applicationContext.getBean(WorkflowInstanceRepository::class.java)
            val workerRegistryRepository = applicationContext.getBean(WorkerRegistryRepository::class.java)
            val taskRepository = applicationContext.getBean(TaskRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                return@setup Behaviors.withTimers { timer ->
                    timer.startTimerWithFixedDelay(
                        Command.ScheduleWorkflows,
                        Duration.ofSeconds(1)
                    )
                    timer.startTimerWithFixedDelay(
                        Command.CreateTasks,
                        Duration.ofSeconds(1)
                    )
                    val jobSchedulerActor = WorkflowSchedulerActor(
                        context = context,
                        serverAddressHolder = serverAddressHolder,
                        jobInstanceRepository = jobInstanceRepository,
                        workflowRepository = workflowRepository,
                        workflowNodeRepository = workflowNodeRepository,
                        workerRegistryRepository = workerRegistryRepository,
                        workflowInstanceRepository = workflowInstanceRepository,
                        taskRepository = taskRepository,
                        transactionTemplate = transactionTemplate,
                    )
                    return@withTimers jobSchedulerActor
                }
            }.apply {
                Behaviors.supervise(this).onFailure(SupervisorStrategy.resume())
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessageEquals(Command.ScheduleWorkflows) { handleScheduleWorkflows() }
            .onMessageEquals(Command.CreateTasks) { handleCreateTasks() }
            .onSignal(PostStop::class.java) { signal -> onPostStop() }
            .build()
    }

    private fun handleScheduleWorkflows(): Behavior<Command> {
        var pageNo = 1
        val currentServerAddress = serverAddressHolder.address
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val workflowIdPage = workflowRepository.listIdsByEnabledAndSchedulerAddress(
                enabled = true,
                schedulerAddress = currentServerAddress,
                pageQuery = pageQuery
            )
            val assignedWorkflows = workflowIdPage.content
            if (assignedWorkflows.isEmpty()) {
                log.debug("工作流任务调度结束: 当前节点没有被分配任何工作流任务")
                continue
            }
            val schedulableList = workflowRepository.findSchedulableByIds(
                ids = assignedWorkflows,
                baseTime = LocalDateTime.now()
            )
            if (schedulableList.isEmpty()) {
                log.debug("工作流任务调度结束: 没有需要调度的工作流任务")
                continue
            }
            val workflowIds = schedulableList.mapNotNull { it.id }
            scheduleWorkflows(workflowIds)
        } while (workflowIdPage.isNotEmpty())
        return this
    }

    private fun scheduleWorkflows(
        workflowIds: List<WorkflowId>,
    ) {
        // 使用管道限制最大并发数量, 为了避免并发大量的请求导致系统资源不足
        val channel = Channel<Unit>(20)
        runBlocking {
            val asyncScheduleJobs = workflowIds.map { workflowId ->
                async {
                    channel.send(Unit)
                    try {
                        schedulerOne(workflowId = workflowId)
                    } catch (e: Exception) {
                        log.error("schedule job [{}] failed: {}", workflowId.value, e.message, e)
                    } finally {
                        channel.receive()
                    }
                }
            }
            asyncScheduleJobs.awaitAll()
        }
    }

    private fun schedulerOne(workflowId: WorkflowId) {
        transactionTemplate.executeWithoutResult {
            val workflow = workflowRepository.lockById(workflowId)
            if (workflow == null) {
                return@executeWithoutResult
            }
            if (workflow.enabled!!.not()) {
                return@executeWithoutResult
            }
            val workflowNodes = workflowNodeRepository.findAllByWorkflow(workflow)
            if (workflowNodes.isEmpty()) {
                return@executeWithoutResult
            }
            // 对于第一次调度的任务, 初始化下次调度时间
            if (workflow.nextScheduleAt == null) {
                workflow.updateNextScheduleTime()
                workflowRepository.save(workflow)
                return@executeWithoutResult
            }
            // 检查任务实例并发数量
            val workflowId2UnfinishedWorkflowInstanceCount = workflowInstanceRepository.countByWorkflowIdAndStatus(
                workflowIds = listOf(workflowId),
                statuses = WorkflowStatusEnum.UNCOMPLETED_STATUSES
            )
            val maxConcurrentNum = workflow.maxConcurrentNum!!
            val existUnfinishedWorkflowInstanceCount = workflowId2UnfinishedWorkflowInstanceCount[workflow.id] ?: 0L
            if (existUnfinishedWorkflowInstanceCount >= maxConcurrentNum) {
                return@executeWithoutResult
            }
            // 检查当前可用机器, 如果没有可用机器，则跳过本次调度(TODO: 系统告警)
            val appGroupKey = AppGroupKey(workflow.appGroup!!)
            val availableWorkers = workerRegistryRepository.findAllByAppGroupKey(appGroupKey)
            if (availableWorkers.isEmpty()) {
                workflow.updateNextScheduleTimeWhenNoAvailableWorker()
                workflowRepository.save(workflow)
                log.info(
                    "schedule workflow [{}] cancel for no available workers, nextScheduleTime={}",
                    workflowId.value, workflow.nextScheduleAt
                )
                return@executeWithoutResult
            }
            // 前置检查全部通过后, 正式开始调度
            val workflowInstance = workflow.createInstance()
            val rootNodeInstances = workflowInstance.workflowNodeInstances.filter { it.parents.isEmpty() }
            val jobInstances = rootNodeInstances.map { it.createJobInstance() }
            workflow.apply {
                /*
                    对于固定延迟的调度模式, 先以本次调度时间为基准设置下次调度时间(避免本次调度的任务完成前, 调度方法总是将任务查出来)
                    在任务完成后再真正更新下次调度时间，之所以可以这么做是因为: 任务完成时间 + 固定延迟 > 任务调度时间 + 固定延迟
                 */
                if (scheduleType == ScheduleTypeEnum.FIX_DELAY) {
                    this.nextScheduleAt = this.nextScheduleAt!!.plusSeconds(this.scheduleConfig!!.toLong())
                } else {
                    updateNextScheduleTime()
                }
            }

            workflowRepository.save(workflow)
            workflowInstanceRepository.save(workflowInstance)
            jobInstanceRepository.saveAll(jobInstances)
            log.info("schedule workflow [{}] success, nextScheduleTime={}", workflowId.value, workflow.nextScheduleAt)
        }
    }

    private fun handleCreateTasks(): Behavior<Command> {
        var pageNo = 1
        val currentServerAddress = serverAddressHolder.address
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val workflowIdPage = workflowRepository.listIdsByEnabledAndSchedulerAddress(
                enabled = null,
                schedulerAddress = currentServerAddress,
                pageQuery = pageQuery
            )
            if (workflowIdPage.isEmpty()) {
                break
            }
            val workflowIds = workflowIdPage.content
            createTasks(workflowIds)
        } while (workflowIdPage.isNotEmpty())
        return this
    }

    private fun createTasks(workflowIds: List<WorkflowId>) {
        var pageNo = 1
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 20)
            val jobInstanceIdPage = jobInstanceRepository.listDispatchable(
                sourceIds = workflowIds.map { it.toSourceId() },
                sourceType = JobSourceTypeEnum.WORKFLOW,
                pageQuery = pageQuery
            )
            if (jobInstanceIdPage.isEmpty()) {
                break
            }
            val jobInstanceIds = jobInstanceIdPage.content
            val appCode2AvailableWorkers = mutableMapOf<AppGroupKey, List<WorkerRegistry>>()
            jobInstanceIds.forEach { jobInstanceId ->
                transactionTemplate.executeWithoutResult {
                    val jobInstance = jobInstanceRepository.lockById(jobInstanceId)
                    if (jobInstance == null) {
                        return@executeWithoutResult
                    }
                    if (jobInstance.jobStatus != JobStatusEnum.WAITING_SCHEDULE) {
                        return@executeWithoutResult
                    }
                    val appGroupKey = AppGroupKey(jobInstance.appGroup!!)
                    val workerRegistries = appCode2AvailableWorkers.computeIfAbsent(appGroupKey) { appGroupKey ->
                        workerRegistryRepository.findAllByAppGroupKey(appGroupKey)
                    }
                    if (workerRegistries.isEmpty()) {
                        if (jobInstance.canReattempt) {
                            jobInstance.resetStatusForReattempt()
                        } else {
                            jobInstance.markFailed(message = "no available workers")
                        }
                        jobInstanceRepository.save(jobInstance)
                        return@executeWithoutResult
                    }
                    jobInstance.jobStatus = JobStatusEnum.WAITING_DISPATCH
                    val tasks = jobInstance.createTasks(workerRegistries)
                    jobInstanceRepository.save(jobInstance)
                    taskRepository.saveAll(tasks)
                }
            }
        } while (jobInstanceIdPage.isNotEmpty())
    }

    private fun onPostStop(): Behavior<Command> {
        log.info("start to reset jobs assigned to this server")
        val currentServerAddress = serverAddressHolder.address
        workflowRepository.clearSchedulerByAddress(currentServerAddress)
        log.info("successfully reset jobs assigned to this server")
        return this
    }
}