package tech.powerscheduler.server.domain.task

import tech.powerscheduler.common.enums.*
import tech.powerscheduler.server.domain.appgroup.AppGroup
import tech.powerscheduler.server.domain.job.JobInstanceId
import tech.powerscheduler.server.domain.job.SourceId
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/6
 */
class Task {

    /**
     * 应用分组信息
     */
    var appGroup: AppGroup? = null

    /**
     * 主键
     */
    var id: TaskId? = null

    /**
     * 父任务id
     */
    var parentId: TaskId? = null

    /**
     * 任务来源对象的id(JobId 或者 WorkflowId)
     */
    var sourceId: SourceId? = null

    /**
     * 任务来源
     */
    var sourceType: JobSourceTypeEnum? = null

    /**
     * 任务实例id
     */
    var jobInstanceId: JobInstanceId? = null

    /**
     * 应用编码
     */
    @Deprecated(message = "replaced by appGroup")
    var appCode: String? = null

    /**
     * 任务名称
     */
    var taskName: String? = null

    /**
     * 任务类型
     */
    var jobType: JobTypeEnum? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 任务状态
     */
    var taskStatus: JobStatusEnum? = null

    /**
     * 调度时间
     */
    var scheduleAt: LocalDateTime? = null

    /**
     * 开始时间
     */
    var startAt: LocalDateTime? = null

    /**
     * 结束时间
     */
    var endAt: LocalDateTime? = null

    /**
     * 任务参数
     */
    var executeParams: String? = null

    /**
     * 执行模式
     */
    var executeMode: ExecuteModeEnum? = null

    /**
     * 调度类型
     */
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 任务结果
     */
    var result: String? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = null

    /**
     * 脚本类型
     */
    var scriptType: ScriptTypeEnum? = null

    /**
     * 脚本源代码
     */
    var scriptCode: String? = null

    /**
     * 当前重试次数
     */
    var attemptCnt: Int? = null

    /**
     * 最大重试次数
     */
    var maxAttemptCnt: Int? = null

    /**
     * 重试间隔(s)
     */
    var attemptInterval: Int? = null

    /**
     * 优先级
     */
    var priority: Int? = null

    /**
     * 调度器地址
     */
    var schedulerAddress: String? = null

    /**
     * 执行器地址
     */
    var workerAddress: String? = null

    /**
     * 批次
     */
    var batch: Int? = null

    /**
     * 子任务内容(用于存储 Map 和 MapReduce 模式下用户自定义的任务参数)
     */
    var taskBody: String? = null

    /**
     * 任务类型
     */
    var taskType: TaskTypeEnum? = null

    val canReattempt
        get() = this.attemptCnt!! < this.maxAttemptCnt!!

    val isCompleted
        get() = JobStatusEnum.COMPLETED_STATUSES.contains(this.taskStatus)

    fun markFailed(message: String? = null) {
        this.startAt = this.startAt ?: LocalDateTime.now()
        this.endAt = LocalDateTime.now()
        this.taskStatus = JobStatusEnum.FAILED
        this.result = message?.take(2000)
    }

    fun resetStatusForReattempt() {
        this.taskStatus = JobStatusEnum.WAITING_DISPATCH
        this.startAt = null
        this.endAt = null
        this.scheduleAt = LocalDateTime.now().plusSeconds(this.attemptInterval?.toLong() ?: 15)
        this.attemptCnt = this.attemptCnt!! + 1
    }

    fun terminate() {
        if (this.startAt == null) {
            this.startAt = LocalDateTime.now()
        }
        if (this.endAt == null) {
            this.endAt = LocalDateTime.now()
        }
        this.taskStatus = JobStatusEnum.CANCELED
    }

    fun createSubTask(subTaskBodyList: List<String>, subTaskName: String): List<Task> {
        return subTaskBodyList.map { subTaskBody ->
            Task().also {
                it.appGroup = this.appGroup
                it.parentId = this.id
                it.sourceId = this.sourceId
                it.sourceType = this.sourceType
                it.jobInstanceId = this.jobInstanceId
                it.taskName = subTaskName
                it.jobType = this.jobType
                it.processor = this.processor
                it.taskStatus = JobStatusEnum.WAITING_DISPATCH
                it.scheduleAt = LocalDateTime.now()
                it.executeParams = this.executeParams
                it.executeMode = this.executeMode
                it.scheduleType = this.scheduleType
                it.dataTime = this.dataTime
                it.attemptCnt = 0
                it.maxAttemptCnt = this.maxAttemptCnt
                it.attemptInterval = this.attemptInterval
                it.priority = this.priority
                it.batch = this.batch
                it.taskBody = subTaskBody
                it.taskType = TaskTypeEnum.SUB
            }
        }
    }

    fun createReduceTask(): Task {
        return Task().also {
            it.appGroup = this.appGroup
            it.sourceId = this.sourceId
            it.sourceType = this.sourceType
            it.jobInstanceId = this.jobInstanceId
            it.taskName = "REDUCE_TASK"
            it.jobType = this.jobType
            it.processor = this.processor
            it.taskStatus = JobStatusEnum.WAITING_DISPATCH
            it.scheduleAt = LocalDateTime.now()
            it.executeParams = this.executeParams
            it.executeMode = this.executeMode
            it.scheduleType = this.scheduleType
            it.dataTime = this.dataTime
            it.attemptCnt = 0
            it.maxAttemptCnt = this.maxAttemptCnt
            it.attemptInterval = this.attemptInterval
            it.priority = this.priority
            it.batch = this.batch
            it.taskType = TaskTypeEnum.REDUCE
        }
    }
}