package tech.powerscheduler.server.application.scheduler

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.ScheduleTypeEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.application.actor.ServerAddressHolder
import tech.powerscheduler.server.application.service.JobInstanceService
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
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/12/13
 */
@Service
class WorkflowScheduler(
    private val serverAddressHolder: ServerAddressHolder,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workflowRepository: WorkflowRepository,
    private val workflowInstanceRepository: WorkflowInstanceRepository,
    private val workflowNodeRepository: WorkflowNodeRepository,
    private val transactionTemplate: TransactionTemplate,
    private val workerRegistryRepository: WorkerRegistryRepository,
    private val taskRepository: TaskRepository,
    private val jobInstanceService: JobInstanceService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun handleScheduleWorkflows() {
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
    }

    fun handleCreateTasks() {
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
    }

    fun resetWorkflowScheduler() {
        log.info("start to reset jobs assigned to this server")
        val currentServerAddress = serverAddressHolder.address
        workflowRepository.clearSchedulerByAddress(currentServerAddress)
        log.info("successfully reset jobs assigned to this server")
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
            // 前置检查全部通过后, 正式开始调度
            val workflowInstance = workflow.createInstance()
            val rootNodeInstances = workflowInstance.workflowNodeInstances.filter { it.parents.isEmpty() }
            val jobInstances = rootNodeInstances.map { it.createJobInstance() }
            workflow.apply {
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
                    val jobInstance = jobInstanceRepository.lockById(jobInstanceId) ?: return@executeWithoutResult
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
                            jobInstanceRepository.save(jobInstance)
                        } else {
                            jobInstance.markFailed(message = "no available workers")
                            jobInstanceRepository.save(jobInstance)
                            jobInstanceService.updateWorkflowInstance(jobInstance)
                        }
                    } else {
                        jobInstance.jobStatus = JobStatusEnum.WAITING_DISPATCH
                        val tasks = jobInstance.createTasks(workerRegistries)
                        jobInstanceRepository.save(jobInstance)
                        taskRepository.saveAll(tasks)
                    }
                }
            }
        } while (jobInstanceIdPage.isNotEmpty())
    }
}