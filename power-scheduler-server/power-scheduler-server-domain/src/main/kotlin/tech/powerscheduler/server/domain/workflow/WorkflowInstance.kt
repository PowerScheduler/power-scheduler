package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.common.enums.ScheduleTypeEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.appgroup.AppGroup
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/21
 */
class WorkflowInstance {
    /**
     * 应用分组
     */
    var appGroup: AppGroup? = null

    /**
     * 工作流节点实例列表
     */
    var workflowNodeInstances: List<WorkflowNodeInstance> = emptyList()

    /**
     * 主键
     */
    var id: WorkflowInstanceId? = null

    /**
     * 工作流id
     */
    var workflowId: WorkflowId? = null

    /**
     * 工作流实例编码
     */
    var code: String? = null

    /**
     * 工作流名称
     */
    var name: String? = null

    /**
     * 状态
     */
    var status: WorkflowStatusEnum? = null

    /**
     * 调度类型
     */
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 调度时间
     */
    var scheduleAt: LocalDateTime? = null

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
     * 有向无环图的UI数据
     */
    var graphData: WorkflowInstanceGraphData? = null

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

    fun updateProgress() {
        val calculatedJobStatus = this.calculateStatus()
        this.status = calculatedJobStatus
        if (this.startAt == null) {
            this.startAt = LocalDateTime.now()
        }
        if (calculatedJobStatus in WorkflowStatusEnum.COMPLETED_STATUSES) {
            this.endAt = LocalDateTime.now()
        }
    }

    fun calculateStatus(): WorkflowStatusEnum {
        val nodeInstanceStatusSet = this.workflowNodeInstances.mapNotNull { it.status }.toSet()
        if (nodeInstanceStatusSet.all { it == WorkflowStatusEnum.SUCCESS }) {
            return WorkflowStatusEnum.SUCCESS
        }
        if (nodeInstanceStatusSet.any { it == WorkflowStatusEnum.FAILED }) {
            return WorkflowStatusEnum.FAILED
        }
        return if (nodeInstanceStatusSet.intersect(WorkflowStatusEnum.COMPLETED_STATUSES).isNotEmpty()) {
            // 如果部分完成, 则设置为执行中
            WorkflowStatusEnum.RUNNING
        } else {
            // 如果没有任何完成状态的, 则取最大的状态
            nodeInstanceStatusSet.maxBy { it.ordinal }
        }
    }
}