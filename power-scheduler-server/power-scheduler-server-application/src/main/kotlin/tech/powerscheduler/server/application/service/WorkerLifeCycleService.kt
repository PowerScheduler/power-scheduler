package tech.powerscheduler.server.application.service

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.common.dto.request.*
import tech.powerscheduler.common.dto.response.FetchTaskResultResponseDTO
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.enums.ExecuteModeEnum
import tech.powerscheduler.common.enums.TaskStatusEnum
import tech.powerscheduler.common.enums.TaskTypeEnum
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.TaskAssembler
import tech.powerscheduler.server.application.assembler.WorkerRegistryAssembler
import tech.powerscheduler.server.application.dto.request.WorkerQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.WorkerQueryResponseDTO
import tech.powerscheduler.server.application.exception.OptimisticLockingConflictException
import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.appgroup.AppGroupKey
import tech.powerscheduler.server.domain.appgroup.AppGroupRepository
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobInstanceId
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.namespace.NamespaceRepository
import tech.powerscheduler.server.domain.task.Task
import tech.powerscheduler.server.domain.task.TaskId
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.task.TaskStatusChangeEvent
import tech.powerscheduler.server.domain.worker.WorkerRegistry
import tech.powerscheduler.server.domain.worker.WorkerRegistryRepository
import tech.powerscheduler.server.domain.worker.WorkerRegistryUniqueKey
import java.time.LocalDateTime

/**
 * worker生命周期管理服务
 *
 * @author grayrat
 * @since 2025/5/21
 */
@Service
class WorkerLifeCycleService(
    private val taskAssembler: TaskAssembler,
    private val taskRepository: TaskRepository,
    private val appGroupRepository: AppGroupRepository,
    private val namespaceRepository: NamespaceRepository,
    private val transactionTemplate: TransactionTemplate,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workerRegistryRepository: WorkerRegistryRepository,
    private val workerRegistryAssembler: WorkerRegistryAssembler,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun list(param: WorkerQueryRequestDTO): List<WorkerQueryResponseDTO> {
        val appGroupKey = AppGroupKey(
            namespaceCode = param.namespaceCode!!,
            appCode = param.appCode!!,
        )
        val workerRegistries = workerRegistryRepository.findAllByAppGroupKey(appGroupKey)
        return workerRegistries.map { workerRegistryAssembler.toWorkerQueryResponseDTO(it) }
    }

    @Retryable(
        value = [OptimisticLockingConflictException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 0)
    )
    fun register(param: WorkerRegisterRequestDTO, remoteHost: String): String {
        checkAppCertificate(
            namespaceCode = param.namespaceCode!!,
            appCode = param.appCode!!,
            appSecret = param.appSecret!!
        )
        return transactionTemplate.execute {
            val existWorkerRegistry = workerRegistryRepository.findByUk(
                WorkerRegistryUniqueKey(
                    host = param.host?.takeIf { it.isNotBlank() } ?: remoteHost,
                    port = param.port!!,
                )
            )
            val accessToken = if (existWorkerRegistry != null) {
                existWorkerRegistry.lastHeartbeatAt = LocalDateTime.now()
                workerRegistryRepository.save(existWorkerRegistry)
                existWorkerRegistry.accessToken
            } else {
                val workerRegistryToSave = workerRegistryAssembler.toDomainModel4RegisterRequestDTO(param, remoteHost)
                workerRegistryRepository.save(workerRegistryToSave)
                workerRegistryToSave.accessToken
            }
            return@execute accessToken
        }!!
    }

    @Retryable(
        value = [OptimisticLockingConflictException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 0)
    )
    fun handleHeartbeat(param: WorkerHeartbeatRequestDTO, remoteAddr: String) {
        val existWorkerRegistry = workerRegistryRepository.findByUk(
            WorkerRegistryUniqueKey(
                host = param.host ?: remoteAddr,
                port = param.port!!,
            )
        )
        if (existWorkerRegistry != null) {
            if (existWorkerRegistry.accessToken != param.accessToken) {
                throw BizException(message = "handle heartbeat failed: invalid accessToken for appCode [${param.appCode}]")
            }
            existWorkerRegistry.lastHeartbeatAt = LocalDateTime.now()
            workerRegistryRepository.save(existWorkerRegistry)
        } else {
            throw BizException("handle heartbeat failed: worker has not registered")
        }
    }

    fun unregister(param: WorkerUnregisterRequestDTO, remoteAddr: String) {
        val uk = WorkerRegistryUniqueKey(
            host = param.host ?: remoteAddr,
            port = param.port!!,
        )
        val existWorkerRegistry = workerRegistryRepository.findByUk(uk)
        if (existWorkerRegistry != null) {
            if (existWorkerRegistry.accessToken != param.accessToken) {
                throw BizException(message = "unregister failed: invalid accessToken for worker [${existWorkerRegistry.address}]")
            }
            removeWorkerRegistry(existWorkerRegistry)
            log.info("unregister worker [{}] successful", existWorkerRegistry.address)
        } else {
            log.warn("worker [{}:{}] has not registered", uk.host, uk.port)
        }
    }

    @Transactional
    fun updateProgress(param: TaskProgressReportRequestDTO) {
        val taskId = TaskId(param.taskId!!)
        val task = taskRepository.lockById(taskId)
        if (task == null) {
            log.warn("updateProgress cancel: task [{}] not exist", taskId.value)
            return
        }
        if (task.taskStatus in TaskStatusEnum.COMPLETED_STATUSES) {
            log.info("updateProgress cancel, task [{}] is already completed", taskId.value)
            return
        }
        task.apply {
            this.taskStatus = param.taskStatus
            this.startAt = param.startAt
            this.endAt = param.endAt
            this.result = param.result?.take(2000)
        }
        if (param.taskStatus == TaskStatusEnum.FAILED && task.canReattempt) {
            task.resetStatusForReattempt()
        }
        val followingTasks = createFollowingTasks(task, param)
        val taskStatusChangeEvent = TaskStatusChangeEvent.create(
            taskId = task.id!!,
            jobInstanceId = task.jobInstanceId!!,
            executeMode = task.executeMode!!,
        )
        taskRepository.save(task)
        if (followingTasks.isNotEmpty()) {
            taskRepository.saveAll(followingTasks)
        }
        log.info("updateProgress successfully: taskId={}, status={}", taskId.value, task.taskStatus)
        applicationEventPublisher.publishEvent(taskStatusChangeEvent)
    }

    private fun createFollowingTasks(
        task: Task,
        param: TaskProgressReportRequestDTO
    ): List<Task> {
        if (needCreateSubTask(task)) {
            return task.createSubTask(
                subTaskBodyList = JSON.splitJsonArrayToObjectStrings(param.subTaskBodyList),
                subTaskName = param.subTaskName.orEmpty(),
            )
        }
        return emptyList()
    }

    fun needCreateSubTask(task: Task): Boolean {
        return task.executeMode in arrayOf(ExecuteModeEnum.MAP, ExecuteModeEnum.MAP_REDUCE)
                && task.taskType in arrayOf(TaskTypeEnum.ROOT, TaskTypeEnum.SUB)
                && task.taskStatus == TaskStatusEnum.SUCCESS
    }

    fun removeWorkerRegistry(workerRegistry: WorkerRegistry) {
        transactionTemplate.executeWithoutResult {
            val existWorkerRegistry = workerRegistryRepository.lockById(workerRegistry.id!!)
            if (existWorkerRegistry == null) {
                return@executeWithoutResult
            }
            val workerAddress = existWorkerRegistry.address

            val uncompletedTaskList = taskRepository.findAllUncompletedByWorkerAddress(workerAddress)
            uncompletedTaskList.forEach {
                if (it.canReattempt) {
                    it.resetStatusForReattempt()
                } else {
                    it.markFailed(message = "worker is offline")
                }
            }
            workerRegistryRepository.delete(existWorkerRegistry.id!!)
            taskRepository.saveAll(uncompletedTaskList)

            uncompletedTaskList.forEach {
                val taskStatusChangeEvent = TaskStatusChangeEvent.create(
                    taskId = it.id!!,
                    jobInstanceId = it.jobInstanceId!!,
                    executeMode = it.executeMode!!,
                )
                applicationEventPublisher.publishEvent(taskStatusChangeEvent)
            }
        }
    }

    fun checkAppCertificate(namespaceCode: String, appCode: String, appSecret: String) {
        val namespace = namespaceRepository.findByCode(namespaceCode)
            ?: throw BizException("namespace is invalid")
        val appGroup = appGroupRepository.findByCode(namespace = namespace, code = appCode)
            ?: throw BizException(message = "appCode is invalid")
        if (appSecret != appGroup.secret) {
            throw BizException(message = "Invalid appCode or appSecret")
        }
    }

    @Retryable(
        value = [OptimisticLockingConflictException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 0)
    )
    fun updateWorkerMetrics(param: WorkerMetricsReportRequestDTO, remoteAddr: String) {
        val uk = WorkerRegistryUniqueKey(
            host = param.host ?: remoteAddr,
            port = param.port!!,
        )
        val existWorkerRegistry = workerRegistryRepository.findByUk(uk)
        if (existWorkerRegistry != null) {
            if (existWorkerRegistry.accessToken != param.accessToken) {
                throw BizException(message = "updateWorkerMetrics failed: invalid accessToken for worker [${existWorkerRegistry.address}]")
            }
            existWorkerRegistry.cpuUsagePercent = param.cpuUsagePercent
            existWorkerRegistry.memoryUsagePercent = param.memoryUsagePercent
            workerRegistryRepository.save(existWorkerRegistry)
        } else {
            log.warn("worker [{}:{}] has not registered", uk.host, uk.port)
        }
    }

    fun fetchTaskResult(param: FetchTaskResultRequestDTO): PageDTO<FetchTaskResultResponseDTO> {
        val jobInstance = jobInstanceRepository.findById(JobInstanceId(param.jobInstanceId!!))
        if (jobInstance == null) {
            return PageDTO.empty(number = param.pageNo, size = param.pageSize)
        }
        val page = taskRepository.findAllByJobInstanceIdAndBatchAndTaskType(
            jobInstanceId = jobInstance.id!!,
            batch = jobInstance.batch!!,
            taskTypes = listOf(TaskTypeEnum.SUB),
            pageQuery = PageQuery(param.pageNo, param.pageSize),
        )
        return page.toDTO().map { taskAssembler.toFetchTaskResultResponseDTO(it) }
    }

}