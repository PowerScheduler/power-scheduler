package tech.powerscheduler.server.domain.job

import tech.powerscheduler.common.enums.*
import tech.powerscheduler.common.enums.ExecuteModeEnum.*
import tech.powerscheduler.server.domain.appgroup.AppGroup
import tech.powerscheduler.server.domain.task.Task
import tech.powerscheduler.server.domain.worker.WorkerRegistry
import java.time.LocalDateTime

/**
 * 任务实例
 *
 * @author grayrat
 * @since 2025/4/16
 */
class JobInstance {

    /**
     * 应用分组信息
     */
    var appGroup: AppGroup? = null

    /**
     * 主键
     */
    var id: JobInstanceId? = null

    /**
     * 任务来源对象的id(JobId 或者 WorkflowId)
     */
    var sourceId: SourceId? = null

    /**
     * 任务来源
     */
    var sourceType: JobSourceTypeEnum? = null

    /**
     * 工作流实例编码
     */
    var workflowInstanceCode: String? = null

    /**
     * 工作流节点实例编码
     */
    var workflowNodeInstanceCode: String? = null

    /**
     * 任务名称
     */
    var jobName: String? = null

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
    var jobStatus: JobStatusEnum? = null

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
     * 任务信息
     */
    var message: String? = null

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
     * 子任务最大重试次数
     */
    var taskMaxAttemptCnt: Int? = null

    /**
     * 子任务重试间隔(s)
     */
    var taskAttemptInterval: Int? = null

    /**
     * 优先级
     */
    var priority: Int? = null

    /**
     * worker地址(指定机器运行时使用)
     */
    var workerAddress: String? = null

    /**
     * 调度器地址
     */
    var schedulerAddress: String? = null

    /**
     * 创建人
     */
    var createdBy: String? = null

    /**
     * 创建时间
     */
    var createdAt: LocalDateTime? = null

    /**
     * 修改人
     */
    var updatedBy: String? = null

    /**
     * 修改时间
     */
    var updatedAt: LocalDateTime? = null

    val batch: Int?
        get() = attemptCnt

    val canReattempt
        get() = this.attemptCnt!! < this.maxAttemptCnt!!

    fun markFailed(message: String? = null) {
        this.startAt = this.startAt ?: LocalDateTime.now()
        this.endAt = LocalDateTime.now()
        this.jobStatus = JobStatusEnum.FAILED
        this.message = message?.take(2000)
    }

    fun resetStatusForReattempt() {
        this.scheduleAt = LocalDateTime.now().plusSeconds(this.attemptInterval?.toLong() ?: 15)
        this.startAt = null
        this.endAt = null
        this.attemptCnt = this.attemptCnt!! + 1
        this.jobStatus = JobStatusEnum.WAITING_SCHEDULE
    }

    fun cloneForRetry(): JobInstance {
        val newInstance = JobInstance().also {
            it.appGroup = this.appGroup
            it.sourceType = this.sourceType
            it.sourceId = this.sourceId
            it.jobName = this.jobName
            it.jobType = this.jobType
            it.processor = this.processor
            it.jobStatus = JobStatusEnum.WAITING_SCHEDULE
            it.scheduleAt = LocalDateTime.now()
            it.executeParams = this.executeParams
            it.executeMode = this.executeMode
            it.scheduleType = this.scheduleType
            it.dataTime = this.dataTime
            it.scriptType = this.scriptType
            it.scriptCode = this.scriptCode
            it.attemptCnt = 0
            it.maxAttemptCnt = 0
            it.attemptInterval = this.attemptInterval
            it.taskMaxAttemptCnt = 0
            it.taskAttemptInterval = 0
            it.priority = this.priority
        }
        return newInstance
    }

    fun createTasks(availableWorkers: List<WorkerRegistry>): List<Task> {
        val tasks = when (executeMode!!) {
            // 单机模式创建1个task
            // Map/MapReduce模式 先创建1个task，后续根据任务上报的结果持续创建子task(需要做好幂等)
            SINGLE, MAP, MAP_REDUCE -> {
                val targetWorkerAddress = selectWorker(candidateWorkers = availableWorkers)
                val task = this.createTask(targetWorkerAddress)
                listOf(task)
            }
            // 广播模式 有多少台在线的worker就创建多少个task
            BROADCAST -> {
                availableWorkers.map { this.createTask(it.address) }
            }
        }
        return tasks
    }

    private fun selectWorker(
        candidateWorkers: List<WorkerRegistry>
    ): String {
        val specifiedWorkerAddress = this.workerAddress
        val executeMode = this.executeMode
        if (executeMode == SINGLE && specifiedWorkerAddress.orEmpty().isNotBlank()) {
            // 使用用户指定的运行机器
            return specifiedWorkerAddress!!
        }
        return if (this.attemptCnt!! > 0 && candidateWorkers.size > 1) {
            // 如果上次任务执行失败了，本次就换个节点执行
            candidateWorkers.asSequence()
                .filterNot { it.address == this.workerAddress }
                .maxBy { it.healthScore }
                .address
        } else {
            candidateWorkers.maxBy { it.healthScore }.address
        }
    }

    fun createTask(workerAddress: String?): Task {
        val task = Task().also {
            it.appGroup = this.appGroup
            it.sourceId = this.sourceId
            it.sourceType = this.sourceType
            it.jobInstanceId = this.id
            it.taskName = this.jobName
            it.jobType = this.jobType
            it.processor = this.processor
            it.taskStatus = TaskStatusEnum.WAITING_DISPATCH
            it.scheduleAt = this.scheduleAt
            it.executeParams = this.executeParams
            it.executeMode = this.executeMode
            it.scheduleType = this.scheduleType
            it.result = this.message
            it.dataTime = this.dataTime
            it.scriptType = this.scriptType
            it.scriptCode = this.scriptCode
            it.priority = this.priority
            it.schedulerAddress = this.schedulerAddress
            it.workerAddress = if (this.executeMode == SINGLE && this.workerAddress.orEmpty().isNotEmpty()) {
                // 单机模式支持指定机器运行
                this.workerAddress
            } else {
                workerAddress
            }
            it.batch = this.batch
            it.attemptCnt = 0
            when (this.executeMode!!) {
                SINGLE -> {
                    it.maxAttemptCnt = 0
                    it.attemptInterval = 0
                    it.taskType = TaskTypeEnum.NORMAL
                }

                BROADCAST -> {
                    it.maxAttemptCnt = this.taskMaxAttemptCnt
                    it.attemptInterval = this.taskAttemptInterval
                    it.taskType = TaskTypeEnum.NORMAL
                }

                MAP, MAP_REDUCE -> {
                    it.taskName = "ROOT_TASK"
                    it.maxAttemptCnt = this.taskMaxAttemptCnt
                    it.attemptInterval = this.taskAttemptInterval
                    it.taskType = TaskTypeEnum.ROOT
                }
            }
        }
        return task
    }

    fun updateProgress(tasks: Iterable<Task>) {
        val calculatedJobStatus = this.calculateJobStatus(tasks)
        this.jobStatus = calculatedJobStatus
        // task可能会失败重试, 开始时间只取第一个task的开始时间
        if (this.startAt == null) {
            this.startAt = this.calculateStartAt(tasks)
        }
        this.workerAddress = this.calculateWorkerAddress(tasks)
        if (calculatedJobStatus in JobStatusEnum.COMPLETED_STATUSES) {
            this.endAt = this.calculateEndAt(tasks)
        }
        if (calculatedJobStatus == JobStatusEnum.FAILED) {
            if (this.canReattempt) {
                this.resetStatusForReattempt()
            } else {
                if (this.executeMode == SINGLE) {
                    this.message = tasks.mapNotNull { it.result }.firstOrNull { it.isNotBlank() }
                }
            }
        }
    }

    fun calculateJobStatus(tasks: Iterable<Task>): JobStatusEnum {
        if (this.jobStatus in JobStatusEnum.COMPLETED_STATUSES) {
            return this.jobStatus!!
        }
        val maxBatch = tasks.maxOfOrNull { it.batch!! }
        return when (this.executeMode!!) {
            SINGLE -> {
                val taskStatus = tasks.first { it.batch == maxBatch }.taskStatus!!
                JobStatusEnum.from(taskStatus)
            }

            BROADCAST, MAP, MAP_REDUCE -> {
                val currentTasks = tasks.filter { it.batch == maxBatch }
                val taskStatusSet = currentTasks.map { it.taskStatus!! }.toSet()
                // 如果全部任务都已经完成, 则设置成功或者失败状态
                if ((taskStatusSet - TaskStatusEnum.COMPLETED_STATUSES).isEmpty()) {
                    if (taskStatusSet.all { it == TaskStatusEnum.SUCCESS }) {
                        JobStatusEnum.SUCCESS
                    } else {
                        JobStatusEnum.FAILED
                    }
                } else {
                    if (taskStatusSet.intersect(TaskStatusEnum.COMPLETED_STATUSES).isNotEmpty()) {
                        // 如果部分完成, 则设置为执行中
                        JobStatusEnum.PROCESSING
                    } else {
                        // 如果没有任何完成状态的, 则取最大的状态
                        JobStatusEnum.from(taskStatusSet.maxBy { it.ordinal })
                    }
                }
            }
        }
    }

    fun calculateStartAt(tasks: Iterable<Task>): LocalDateTime? {
        return tasks.asSequence()
            .mapNotNull { it.startAt }
            .minOrNull()
    }

    fun calculateEndAt(tasks: Iterable<Task>): LocalDateTime? {
        val maxBatch = tasks.maxOfOrNull { it.batch!! }
        return tasks.asSequence()
            .filter { it.batch == maxBatch }
            .mapNotNull { it.endAt }
            .maxOrNull()
    }

    fun calculateWorkerAddress(tasks: Iterable<Task>): String? {
        val maxBatch = tasks.maxOfOrNull { it.batch!! }
        // 只有单机模式需要将task中的worker地址回写
        if (this.executeMode == SINGLE) {
            return tasks.firstOrNull { it.batch == maxBatch }?.workerAddress
        }
        return null
    }

    fun terminate() {
        if (this.startAt == null) {
            this.startAt = LocalDateTime.now()
        }
        if (this.endAt == null) {
            this.endAt = LocalDateTime.now()
        }
        this.jobStatus = JobStatusEnum.FAILED
    }
}