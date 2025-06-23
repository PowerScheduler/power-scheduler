package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.common.enums.*
import tech.powerscheduler.server.domain.job.JobInstance
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/21
 */
class WorkflowNodeInstance {

    /**
     * 工作流
     */
    var workflowInstance: WorkflowInstance? = null

    /**
     * 父节点
     */
    var parents: Set<WorkflowNodeInstance> = emptySet()

    /**
     * 子节点
     */
    var children: Set<WorkflowNodeInstance> = emptySet()

    /**
     * 主键
     */
    var id: WorkflowNodeInstanceId? = null

    /**
     * 节点编码
     */
    var nodeCode: String? = null

    /**
     * 节点实例编号
     */
    var nodeInstanceCode: String? = null

    /**
     * 节点名称
     */
    var name: String? = null

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
    var status: WorkflowStatusEnum? = null

    /**
     * 执行模式
     */
    var executeMode: ExecuteModeEnum? = null

    /**
     * 任务参数
     */
    var executeParams: String? = null

    /**
     * 脚本类型
     */
    var scriptType: ScriptTypeEnum? = null

    /**
     * 脚本源代码
     */
    var scriptCode: String? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = null

    /**
     * 开始时间
     */
    var startAt: LocalDateTime? = null

    /**
     * 结束时间
     */
    var endAt: LocalDateTime? = null

    /**
     * Worker地址（ip:host）
     */
    var workerAddress: String? = null

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

    fun createJobInstance(): JobInstance {
        return JobInstance().also {
            val workflowInstance = this.workflowInstance!!
            it.appGroup = workflowInstance.appGroup
            it.sourceId = workflowInstance.workflowId!!.toSourceId()
            it.sourceType = JobSourceTypeEnum.WORKFLOW
            it.workflowInstanceCode = workflowInstance.code
            it.workflowNodeInstanceCode = this.nodeInstanceCode
            it.jobName = this.name
            it.jobType = this.jobType
            it.processor = this.processor
            it.jobStatus = JobStatusEnum.WAITING_SCHEDULE
            it.scheduleAt = workflowInstance.scheduleAt
            it.executeParams = this.executeParams
            it.executeMode = this.executeMode
            it.scheduleType = workflowInstance.scheduleType
            it.dataTime = this.dataTime
            it.scriptType = this.scriptType
            it.scriptCode = this.scriptCode
            it.attemptCnt = 0
            it.maxAttemptCnt = this.maxAttemptCnt
            it.attemptInterval = this.attemptInterval
            it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
            it.taskAttemptInterval = this.taskAttemptInterval
            it.priority = this.priority
            it.workerAddress = this.workerAddress
        }
    }
}