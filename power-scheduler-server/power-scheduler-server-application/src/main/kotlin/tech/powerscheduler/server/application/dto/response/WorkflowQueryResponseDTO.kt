package tech.powerscheduler.server.application.dto.response

/**
 * @author grayrat
 * @since 2025/6/23
 */
class WorkflowQueryResponseDTO {
    /**
     * 命名空间编码
     */
    var namespaceCode: String? = null

    /**
     * 工作流分组编码
     */
    var workflowGroupCode: String? = null

    /**
     * 工作流分组名称
     */
    var workflowGroupName: String? = null

    /**
     * 主键
     */
    var id: Long? = null

    /**
     * 工作流名称
     */
    var name: String? = null

    /**
     * 启用状态
     */
    var enabled: Boolean? = null

    /**
     * 调度类型
     */
    var scheduleType: EnumDTO? = null

    /**
     * 调度配置
     */
    var scheduleConfig: String? = null

    /**
     * 调度配置描述
     */
    var scheduleConfigDesc: String? = null
}