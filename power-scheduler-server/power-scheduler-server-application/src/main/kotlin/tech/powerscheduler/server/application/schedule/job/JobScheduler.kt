package tech.powerscheduler.server.application.schedule.job

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.ScheduleTypeEnum
import tech.powerscheduler.server.application.schedule.system.ServerAddressHolder
import tech.powerscheduler.server.domain.appgroup.AppGroupKey
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobId
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.scheduler.Scheduler
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.worker.WorkerRegistry
import tech.powerscheduler.server.domain.worker.WorkerRegistryRepository
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/12/13
 */
@Component
class JobScheduler(
    private val jobInfoRepository: JobInfoRepository,
    private val schedulerRepository: SchedulerRepository,
    private val serverAddressHolder: ServerAddressHolder,
    private val taskRepository: TaskRepository,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workerRegistryRepository: WorkerRegistryRepository,
    private val transactionTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun assignJobs() {
        val availableSchedulers = schedulerRepository.findAll().filter { it.expired.not() }
        if (availableSchedulers.isEmpty()) {
            log.error("assignWorkflows failed, no available schedulers")
        }
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 200)
            val page = jobInfoRepository.listAssignableIds(query)
            val jobIds = page.content
            reassignJobs(jobIds, availableSchedulers)
        } while (page.isNotEmpty())
    }

    fun reassignJobs() {
        val availableSchedulers = schedulerRepository.findAll().filter { it.expired.not() }
        if (availableSchedulers.isEmpty()) {
            log.error("handleReassignAllJob failed, no available schedulers")
        }
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 1000)
            val page = jobInfoRepository.listAllIds(query)
            val jobIds = page.content
            reassignJobs(jobIds, availableSchedulers)
        } while (page.isNotEmpty())
    }

    private fun reassignJobs(jobIds: List<JobId>, availableSchedulers: List<Scheduler>) {
        if (jobIds.isEmpty()) {
            return
        }
        jobIds.forEach { jobId ->
            try {
                transactionTemplate.executeWithoutResult {
                    val jobInfo = jobInfoRepository.lockById(jobId) ?: return@executeWithoutResult
                    val schedulerIdx = jobIds.size % availableSchedulers.size
                    val assignedScheduler = availableSchedulers[schedulerIdx]
                    jobInfo.schedulerAddress = assignedScheduler.address
                    jobInfoRepository.save(jobInfo)
                    log.info("assign job [{}] to server [{}]", jobInfo.id!!.value, jobInfo.schedulerAddress)
                }
            } catch (e: Exception) {
                log.error("Failed to reassignJobs job [{}]: {}", jobId.value, e.message, e)
            }
        }
    }

    fun handleScheduleDueJobs() {
        var pageNo = 1
        val currentServerAddress = serverAddressHolder.address
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val assignedJobIdPage = jobInfoRepository.listIdsByEnabledAndSchedulerAddress(
                enabled = true,
                schedulerAddress = currentServerAddress,
                pageQuery = pageQuery
            )
            val assignedJobInfos = assignedJobIdPage.content
            if (assignedJobInfos.isEmpty()) {
                continue
            }
            val schedulableList = jobInfoRepository.findSchedulableByIds(
                ids = assignedJobInfos,
                baseTime = LocalDateTime.now()
            )
            if (schedulableList.isEmpty()) {
                continue
            }
            val jobIds = schedulableList.mapNotNull { it.id }
            scheduleJobs(jobIds, currentServerAddress)
        } while (assignedJobIdPage.isNotEmpty())
    }

    private fun scheduleJobs(
        jobIds: List<JobId>,
        currentServerAddress: String
    ) {
        // 使用管道限制最大并发数量, 为了避免并发大量的请求导致系统资源不足
        val channel = Channel<Unit>(20)
        runBlocking {
            val asyncScheduleJobs = jobIds.map { jobId ->
                async {
                    channel.send(Unit)
                    try {
                        schedulerOne(
                            jobId = jobId,
                            currentServerAddress = currentServerAddress
                        )
                    } catch (e: Exception) {
                        log.error("schedule job [{}] failed: {}", jobId.value, e.message, e)
                    } finally {
                        channel.receive()
                    }
                }
            }
            asyncScheduleJobs.awaitAll()
        }
    }

    fun schedulerOne(
        jobId: JobId,
        currentServerAddress: String,
    ) {
        transactionTemplate.executeWithoutResult {
            val jobInfoToSchedule = jobInfoRepository.lockById(jobId)
            if (jobInfoToSchedule == null) {
                return@executeWithoutResult
            }
            if (jobInfoToSchedule.enabled!!.not()) {
                return@executeWithoutResult
            }
            // 对于第一次调度的任务, 初始化下次调度时间
            if (jobInfoToSchedule.nextScheduleAt == null) {
                jobInfoToSchedule.updateNextScheduleTime()
                jobInfoRepository.save(jobInfoToSchedule)
                return@executeWithoutResult
            }
            // 检查任务实例并发数量
            val jobId2UnfinishedJobInstanceCount = jobInstanceRepository.countByJobIdAndJobStatus(
                jobIds = listOf(jobId),
                jobStatuses = JobStatusEnum.UNCOMPLETED_STATUSES
            )
            val maxConcurrentNum = jobInfoToSchedule.maxConcurrentNum!!
            val existUnfinishedJobInstanceCount = jobId2UnfinishedJobInstanceCount[jobInfoToSchedule.id] ?: 0L
            if (existUnfinishedJobInstanceCount >= maxConcurrentNum) {
                return@executeWithoutResult
            }
            // 检查当前可用机器, 如果没有可用机器，则跳过本次调度(TODO: 系统告警)
            val appGroupKey = AppGroupKey(jobInfoToSchedule.appGroup!!)
            val availableWorkers = workerRegistryRepository.findAllByAppGroupKey(appGroupKey)
            if (availableWorkers.isEmpty()) {
                jobInfoToSchedule.apply {
                    // 固定延迟的调度模式由于调度取消无法更新上次完成时间, 所以用本次调度时间为基准设置下次调度时间
                    if (scheduleType == ScheduleTypeEnum.FIX_DELAY) {
                        if (this.nextScheduleAt!! < LocalDateTime.now()) {
                            this.nextScheduleAt = LocalDateTime.now()
                        }
                        this.nextScheduleAt = this.nextScheduleAt!!.plusSeconds(this.scheduleConfig!!.toLong())
                    } else {
                        this.updateNextScheduleTime()
                    }
                }
                jobInfoRepository.save(jobInfoToSchedule)
                log.info(
                    "schedule job [{}] cancel for no available workers, nextScheduleTime={}",
                    jobId.value, jobInfoToSchedule.nextScheduleAt
                )
                return@executeWithoutResult
            }
            // 前置检查全部通过后, 正式开始调度
            val jobInstance = jobInfoToSchedule.createInstance()
            jobInstance.jobStatus = JobStatusEnum.WAITING_SCHEDULE
            jobInstance.schedulerAddress = currentServerAddress
            jobInfoToSchedule.apply {
                /*
                    对于固定延迟的调度模式, 先以本次调度时间为基准设置下次调度时间(避免本次调度的任务完成前, 调度方法总是将任务查出来)
                    在任务完成后再真正更新下次调度时间，之所以可以这么做是: 任务完成时间 + 固定延迟 > 任务调度时间 + 固定延迟
                 */
                if (scheduleType == ScheduleTypeEnum.FIX_DELAY) {
                    this.nextScheduleAt = this.nextScheduleAt!!.plusSeconds(this.scheduleConfig!!.toLong())
                } else {
                    updateNextScheduleTime()
                }
            }
            jobInfoRepository.save(jobInfoToSchedule)
            jobInstanceRepository.save(jobInstance)
            log.info("schedule job [{}] success, nextScheduleTime={}", jobId.value, jobInfoToSchedule.nextScheduleAt)
        }
    }

    fun handleCreateTasks() {
        var pageNo = 1
        val currentServerAddress = serverAddressHolder.address
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val jobIdPage = jobInfoRepository.listIdsByEnabledAndSchedulerAddress(
                enabled = null,
                schedulerAddress = currentServerAddress,
                pageQuery = pageQuery
            )
            if (jobIdPage.isEmpty()) {
                break
            }
            val jobIds = jobIdPage.content
            createTasks(jobIds)
        } while (jobIdPage.isNotEmpty())
    }

    private fun createTasks(jobIds: List<JobId>) {
        var pageNo = 1
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val jobInstanceIdPage = jobInstanceRepository.listDispatchable(
                sourceIds = jobIds.map { it.toSourceId() },
                sourceType = JobSourceTypeEnum.JOB,
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

    fun onPostStop() {
        log.info("start to reset jobs assigned to this server")
        val currentServerAddress = serverAddressHolder.address
        jobInfoRepository.clearSchedulerByAddress(currentServerAddress)
        log.info("successfully reset jobs assigned to this server")
    }
}