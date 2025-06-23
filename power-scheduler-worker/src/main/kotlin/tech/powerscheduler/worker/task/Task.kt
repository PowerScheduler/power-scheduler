package tech.powerscheduler.worker.task

import org.slf4j.LoggerFactory
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.worker.exception.PowerSchedulerWorkerException
import tech.powerscheduler.worker.persistence.TaskProgressEntity
import tech.powerscheduler.worker.persistence.TaskProgressRepository
import tech.powerscheduler.worker.processor.ProcessResult
import tech.powerscheduler.worker.processor.ProcessorRegistry
import tech.powerscheduler.worker.util.JsonUtil.writeValueAsString
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

/**
 * 任务核心类型, 封装了任务的执行流程与生命周期管理
 *
 * @author grayrat
 * @since 2025/4/26
 */
class Task(
    /**
     * 任务上下文
     */
    val context: TaskContext,
    /**
     * 调度时间
     */
    val scheduleAt: LocalDateTime,
    /**
     * 处理器路径
     */
    val processorPath: String,
    /**
     * 任务优先级
     */
    val priority: Int,
) : Delayed {

    private val log = LoggerFactory.getLogger(Task::class.qualifiedName)

    /**
     * 任务的执行线程
     */
    private var workerThread: Thread? = null

    /**
     * 终止标志
     */
    @Volatile
    private var terminated: Boolean = false

    /**
     * 任务当前状态
     */
    var jobStatus: JobStatusEnum = JobStatusEnum.PENDING

    /**
     * 任务开始时间
     */
    var startAt: LocalDateTime? = null

    /**
     * 任务结束时间
     */
    var endAt: LocalDateTime? = null

    /**
     * 执行任务
     */
    fun execute() {
        this.startAt = LocalDateTime.now()
        workerThread = Thread.currentThread()
        updateProgress(jobStatus = JobStatusEnum.PROCESSING)
        try {
            if (terminated) {
                throw PowerSchedulerWorkerException("[Powerscheduler] Job is terminated")
            }
            val processor = ProcessorRegistry.find(processorPath)
                ?: throw PowerSchedulerWorkerException(message = "[Powerscheduler] Processor [$processorPath] not exists")
            val processResult = processor.process(context)
            when (processResult) {
                is ProcessResult.Success -> {
                    updateProgress(jobStatus = JobStatusEnum.SUCCESS, result = processResult.result)
                }

                is ProcessResult.Failure -> {
                    updateProgress(jobStatus = JobStatusEnum.FAILED, result = processResult.message)
                }

                is ProcessResult.Map -> {
                    updateProgress(
                        jobStatus = JobStatusEnum.SUCCESS,
                        subTaskList = processResult.taskList,
                        subTaskName = processResult.taskName,
                    )
                }

                null -> throw PowerSchedulerWorkerException("[Powerscheduler] ProcessResult can not be null")
            }
        } catch (_: InterruptedException) {
            log.info("[Powerscheduler] execution of jobInstance [{}] is canceled", context.jobInstanceId)
            updateProgress(jobStatus = JobStatusEnum.FAILED)
        } catch (e: PowerSchedulerWorkerException) {
            log.error(
                "[Powerscheduler] Error while executing jobInstance [{}]: {}", context.jobInstanceId, e.message, e
            )
            updateProgress(jobStatus = JobStatusEnum.FAILED, result = e.message)
        } catch (e: Throwable) {
            log.error("[Powerscheduler] Error while executing jobInstance [{}]", context.jobInstanceId, e)
            updateProgress(jobStatus = JobStatusEnum.FAILED, result = e.stackTraceToString())
        }
    }

    /**
     * 终止任务
     */
    fun terminate() {
        terminated = false
        workerThread?.interrupt()
    }

    /**
     * 更新任务进度
     *
     * @param jobStatus     任务状态
     * @param result        任务执行结果或者错误信息
     * @param subTaskList   子任务列表
     * @param subTaskName   子任务名称
     */
    private fun updateProgress(
        jobStatus: JobStatusEnum,
        result: Any? = null,
        subTaskList: Iterable<Any> = emptyList(),
        subTaskName: String = ""
    ) {
        this.jobStatus = jobStatus
        if (jobStatus in JobStatusEnum.COMPLETED_STATUSES) {
            this.endAt = LocalDateTime.now()
        }
        val taskProgressEntity = TaskProgressEntity().also {
            it.jobInstanceId = this.context.jobInstanceId!!
            it.taskId = this.context.taskId!!
            it.status = this.jobStatus
            it.startAt = this.startAt
            it.endAt = this.endAt
            it.result = result?.let(::writeValueAsString)
            it.subTaskListBody = writeValueAsString(subTaskList)
            it.subTaskName = subTaskName
        }
        TaskProgressRepository.save(taskProgressEntity)
    }

    override fun getDelay(unit: TimeUnit): Long {
        val delay = ChronoUnit.MILLIS.between(LocalDateTime.now(), scheduleAt)
        return unit.convert(delay, TimeUnit.MILLISECONDS)
    }

    override fun compareTo(other: Delayed?): Int {
        if (other === this) {
            return 0
        }
        val otherTask = other as Task
        return Comparator.comparing<Task, LocalDateTime?> { it!!.scheduleAt }
            .thenComparing(Comparator.comparingInt<Task> { obj: Task -> obj.priority }.reversed())
            .compare(this, otherTask)
    }
}
