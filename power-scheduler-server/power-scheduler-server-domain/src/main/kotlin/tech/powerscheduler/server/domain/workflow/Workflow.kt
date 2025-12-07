package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.common.enums.RetentionPolicyEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.common.Schedulable
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroup
import java.time.LocalDateTime
import java.util.*

/**
 * @author grayrat
 * @since 2025/6/21
 */
class Workflow : Schedulable() {

    /**
     * 工作流分组
     */
    var workflowGroup: WorkflowGroup? = null

    /**
     * 工作流节点
     */
    var workflowNodes: List<WorkflowNode> = emptyList()

    /**
     * 主键
     */
    var id: WorkflowId? = null

    /**
     * 工作流名称
     */
    var name: String? = null

    /**
     * 工作流描述
     */
    var description: String? = null

    /**
     * 启用状态
     */
    var enabled: Boolean? = null

    /**
     * 调度器地址
     */
    var schedulerAddress: String? = null

    /**
     * 并发数
     */
    var maxConcurrentNum: Int? = null

    /**
     * 保留策略
     */
    var retentionPolicy: RetentionPolicyEnum? = null

    /**
     * 保留值
     */
    var retentionValue: Int? = null

    /**
     * 有向无环图的UI数据
     */
    var graphData: WorkflowGraphData? = null

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

    fun createInstance(
        scheduleAt: LocalDateTime? = null,
        dataTime: LocalDateTime = LocalDateTime.now(),
    ): WorkflowInstance {
        return WorkflowInstance().also {
            val workflowNode2Instance = this.workflowNodes.associateWith { workflowNode ->
                workflowNode.createInstance(it)
            }
            for (workflowNode in this.workflowNodes) {
                val nodeInstance = workflowNode2Instance[workflowNode]!!
                nodeInstance.dataTime = dataTime
                nodeInstance.children = workflowNode.children.map { child -> workflowNode2Instance[child]!! }.toSet()
                nodeInstance.parents = workflowNode.parents.map { child -> workflowNode2Instance[child]!! }.toSet()
            }
            val nodeInstances = workflowNode2Instance.values.toList()
            val nodeCode2NodeInstance = nodeInstances.associateBy { nodeInstance -> nodeInstance.nodeCode }

            it.workflowGroup = this.workflowGroup
            it.code = UUID.randomUUID().toString()
            it.workflowId = this.id
            it.workflowNodeInstances = nodeInstances
            it.name = this.name
            it.graphData = WorkflowInstanceGraphData.from(this.graphData!!)
            it.graphData!!
                .filter { graphDataItem -> graphDataItem.shape == "workflow-node-instance" }
                .mapNotNull { graphDataItem -> graphDataItem.data }
                .onEach { data ->
                    val nodeInstance = nodeCode2NodeInstance[data.workflowNodeCode]!!
                    data.workflowInstanceCode = it.code
                    data.workflowNodeInstanceCode = nodeInstance.nodeInstanceCode
                }
            it.status = WorkflowStatusEnum.WAITING
            it.scheduleType = this.scheduleType
            it.scheduleAt = scheduleAt ?: this.nextScheduleAt
            it.dataTime = dataTime
        }
    }
}