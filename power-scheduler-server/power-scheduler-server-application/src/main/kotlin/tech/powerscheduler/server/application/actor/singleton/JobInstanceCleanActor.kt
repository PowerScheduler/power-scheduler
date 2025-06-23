package tech.powerscheduler.server.application.actor.singleton

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.RetentionPolicyEnum
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobInfo
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.task.TaskRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * @author grayrat
 * @since 2025/6/4
 */
class JobInstanceCleanActor(
    context: ActorContext<Command>,
    val jobInfoRepository: JobInfoRepository,
    val jobInstanceRepository: JobInstanceRepository,
    val taskRepository: TaskRepository,
) : AbstractBehavior<JobInstanceCleanActor.Command>(context) {

    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface Command {
        object CleanJobInstance : Command
    }

    companion object {
        fun create(applicationContext: ApplicationContext): Behavior<Command> {
            val jobInfoRepository = applicationContext.getBean(JobInfoRepository::class.java)
            val jobInstanceRepository = applicationContext.getBean(JobInstanceRepository::class.java)
            val taskRepository = applicationContext.getBean(TaskRepository::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = JobInstanceCleanActor(
                        context = context,
                        jobInfoRepository = jobInfoRepository,
                        jobInstanceRepository = jobInstanceRepository,
                        taskRepository = taskRepository,
                    )
                    timer.startTimerWithFixedDelay(
                        Command.CleanJobInstance,
                        Command.CleanJobInstance,
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
            .onMessageEquals(Command.CleanJobInstance, this::handleCleanJobInstance)
            .build()
    }

    fun handleCleanJobInstance(): Behavior<Command> {
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 50)
            val page = jobInfoRepository.listAllIds(query)
            val jobIds = page.content
            val jobInfos = jobInfoRepository.findAllByIds(jobIds)
            cleanJobInstance(jobInfos)
        } while (page.isNotEmpty())
        log.info("clean jobJobInstance successfully")
        return this
    }

    fun cleanJobInstance(jobInfos: Iterable<JobInfo>) {
        val retentionPolicy2JobInfos = jobInfos
            .filter { it.retentionPolicy != null }
            .groupBy { it.retentionPolicy }
        cleanByRetainRecentCountPolicy(retentionPolicy2JobInfos[RetentionPolicyEnum.RECENT_COUNT].orEmpty())
        cleanByRecentDayPolicy(retentionPolicy2JobInfos[RetentionPolicyEnum.RECENT_DAYS].orEmpty())
    }

    fun cleanByRetainRecentCountPolicy(jobInfos: Iterable<JobInfo>) {
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

            var pageNo = 1
            val batchSize = 500L
            val retainCount = jobInfo.retentionValue ?: 300
            var remaining = count - retainCount
            while (remaining > 0) {
                val pageSize = minOf(batchSize, remaining).toInt()
                val pageQuery = PageQuery(
                    pageNo = pageNo++,
                    pageSize = pageSize
                )
                val page = jobInstanceRepository.listIdByJobIdAndJobStatus(
                    jobId = jobId,
                    jobStatuses = JobStatusEnum.COMPLETED_STATUSES,
                    pageQuery = pageQuery,
                )
                if (page.isEmpty()) {
                    break
                }
                jobInstanceRepository.deleteByIds(page.content)
                remaining -= pageSize
            }
            log.info("clean [{}] jobInstance, job=[{}], retainCount={}", count, jobId.value, jobInfo.retentionValue)
        }
    }

    fun cleanByRecentDayPolicy(jobInfos: Iterable<JobInfo>) {
        val batchSize = 500
        val now = LocalDateTime.now()

        for (jobInfo in jobInfos) {
            val retentionDays = jobInfo.retentionValue ?: 15
            val jobId = jobInfo.id!!
            val expireTime = now.minusDays(retentionDays.toLong()).truncatedTo(ChronoUnit.DAYS)

            var pageNo = 1
            var totalCount = 0
            while (true) {
                val pageQuery = PageQuery(
                    pageNo = pageNo++,
                    pageSize = batchSize
                )
                val page = jobInstanceRepository.listIdByJobIdAndJobStatusAndEndAtBefore(
                    jobId = jobId,
                    jobStatuses = JobStatusEnum.COMPLETED_STATUSES,
                    endAt = expireTime,
                    pageQuery = pageQuery
                )
                if (page.isEmpty()) {
                    break
                }
                totalCount += page.size
                val jobInstanceIds = page.content
                taskRepository.deleteByJobInstanceId(jobInstanceIds = jobInstanceIds)
                jobInstanceRepository.deleteByIds(jobInstanceIds)
            }
            log.info("clean [{}] jobInstance, jobId=[{}], expireTime={}", totalCount, jobId.value, expireTime)
        }
    }

}