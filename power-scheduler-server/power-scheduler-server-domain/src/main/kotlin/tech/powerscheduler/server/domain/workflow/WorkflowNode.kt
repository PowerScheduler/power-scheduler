package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.common.enums.ExecuteModeEnum
import tech.powerscheduler.common.enums.JobTypeEnum
import tech.powerscheduler.common.enums.ScriptTypeEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import java.time.LocalDateTime
import java.util.*


/**
 * @author grayrat
 * @since 2025/6/21
 */
class WorkflowNode {

    /**
     * 工作流
     */
    var workflow: Workflow? = null

    /**
     * 父节点集合
     */
    var parents: Set<WorkflowNode> = emptySet()

    /**
     * 子节点集合
     */
    var children: Set<WorkflowNode> = emptySet()

    /**
     * 主键
     */
    var id: WorkflowNodeId? = null

    /**
     * 节点编码
     */
    var code: String? = null

    /**
     * 节点名称
     */
    var name: String? = null

    /**
     * 节点描述
     */
    var description: String? = null

    /**
     * 任务类型
     */
    var jobType: JobTypeEnum? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

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

    fun createInstance(workflowInstance: WorkflowInstance): WorkflowNodeInstance {
        return WorkflowNodeInstance().also {
            it.workflowInstance = workflowInstance
            it.nodeCode = this.code
            it.nodeInstanceCode = UUID.randomUUID().toString()
            it.name = this.name
            it.jobType = this.jobType
            it.processor = this.processor
            it.executeMode = this.executeMode
            it.executeParams = this.executeParams
            it.scriptType = this.scriptType
            it.scriptCode = this.scriptCode
            it.dataTime = workflowInstance.dataTime
            it.maxAttemptCnt = this.maxAttemptCnt
            it.attemptInterval = this.attemptInterval
            it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
            it.taskAttemptInterval = this.taskAttemptInterval
            it.priority = this.priority
            it.status = WorkflowStatusEnum.WAITING
        }
    }
}