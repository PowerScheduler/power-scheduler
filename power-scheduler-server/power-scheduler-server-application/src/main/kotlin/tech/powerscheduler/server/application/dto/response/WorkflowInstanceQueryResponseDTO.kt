package tech.powerscheduler.server.application.dto.response

import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/9
 */
class WorkflowInstanceQueryResponseDTO {
    /**
     * 应用编码
     */
    var appCode: String? = null

    /**
     * 应用名称
     */
    var appName: String? = null

    /**
     * 工作流实例id
     */
    var id: Long? = null

    /**
     * 工作流id
     */
    var workflowId: Long? = null

    /**
     * 工作流实例名称
     */
    var name: String? = null

    /**
     * 工作流实例编码
     */
    var code: String? = null

    /**
     * 状态
     */
    var status: WorkflowStatusDTO? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = null

    /**
     * 任务开始时间
     */
    var startAt: LocalDateTime? = null

    /**
     * 任务结束时间
     */
    var endAt: LocalDateTime? = null
}