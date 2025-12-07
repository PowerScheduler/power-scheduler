package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotBlank

/**
 * 工作流分组查询请求参数
 *
 * @author grayrat
 * @since 2025/12/7
 */
class WorkflowGroupQueryRequestDTO {
    /**
     * 命名空间编码
     */
    @NotBlank
    var namespaceCode: String? = null

    /**
     * 工作流分组编码
     */
    var code: String? = null

    /**
     * 工作流分组名称
     */
    var name: String? = null
}