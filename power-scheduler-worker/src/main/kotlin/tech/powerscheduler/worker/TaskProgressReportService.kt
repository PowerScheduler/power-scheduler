package tech.powerscheduler.worker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import tech.powerscheduler.common.dto.request.TaskProgressReportRequestDTO
import tech.powerscheduler.worker.persistence.TaskProgressRepository
import tech.powerscheduler.worker.util.ExecutorCoroutineScope

/**
 * 任务进度上报服务
 *
 * @author grayrat
 * @since 2025/5/25
 */
class TaskProgressReportService(
    /**
     * worker注册服务
     */
    private val workerRegisterService: WorkerRegisterService,
    /**
     * server发现服务
     */
    private val serverDiscoveryService: ServerDiscoveryService,
) {

    /**
     * 启用标志
     */
    @Volatile
    private var isActive = true

    /**
     * Http客户端
     */
    private val httpClient = PowerSchedulerWorkerHttpClient()

    private val log = LoggerFactory.getLogger(TaskProgressReportService::class.java)

    /**
     * 启用任务进度上报服务
     */
    fun start() = ExecutorCoroutineScope.launch {
        while (isActive) {
            try {
                this.reportProgress()
            } catch (_: CancellationException) {
                log.info("[Powerscheduler] reportProgress canceled")
            } catch (e: Exception) {
                log.warn("[Powerscheduler] reportProgress failed: {}", e.message, e)
            }
        }
    }

    /**
     * 关闭任务进度上报服务
     */
    fun stop() {
        isActive = false
        ExecutorCoroutineScope.cancelJobs()
        log.info("[PowerScheduler] {} stopped", javaClass.simpleName)
    }

    suspend fun CoroutineScope.reportProgress() {
        val taskIdSet = TaskProgressRepository.listDistinctJobInstanceIds()
        if (taskIdSet.isEmpty()) {
            delay(300)
            return
        }
        if (serverDiscoveryService.availableServerUrls.isEmpty()) {
            delay(300)
            return
        }
        val channel = Channel<Unit>(10)
        val deferredList = taskIdSet.map { taskId ->
            async {
                try {
                    channel.send(Unit)
                    doReportProgress(taskId)
                } catch (e: Exception) {
                    log.warn("[Powerscheduler] reportProgress failed: {}", e.message, e)
                } finally {
                    channel.receive()
                }
            }
        }
        delay(500)
        deferredList.awaitAll()
    }

    fun doReportProgress(taskId: Long) {
        val serverUrl = serverDiscoveryService.availableServerUrls.randomOrNull()
        if (serverUrl == null) {
            log.warn("[Powerscheduler] reportProgress failed: no available server]")
            return
        }
        val taskProgressList = TaskProgressRepository.listByTaskId(taskId)
        val latestProgress = taskProgressList.sortedByDescending { it.id }.first()
        val param = TaskProgressReportRequestDTO().apply {
            this.jobInstanceId = latestProgress.jobInstanceId
            this.taskId = latestProgress.taskId
            this.startAt = latestProgress.startAt
            this.endAt = latestProgress.endAt
            this.taskStatus = latestProgress.status
            this.result = latestProgress.result
            this.accessToken = workerRegisterService.accessToken
            this.subTaskBodyList = latestProgress.subTaskListBody
            this.subTaskName = latestProgress.subTaskName
        }
        val result = httpClient.reportProgress(baseUrl = serverUrl, param = param)
        if (result.success && result.data == true) {
            log.debug(
                "[Powerscheduler] reportProgress successful: jobInstanceId={}, jobStatus={}",
                taskId, latestProgress.status
            )
            val ids = taskProgressList.mapNotNull { it.id }
            TaskProgressRepository.deleteByIds(ids)
        } else {
            log.warn("[Powerscheduler] reportProgress failed: {}", result.message, result.cause)
        }
    }
}