package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotBlank

/**
 * 工作流分组新增请求参数
 *
 * @author grayrat
 * @since 2025/12/7
 */
class WorkflowGroupAddRequestDTO {
    /**
     * 命名空间编码
     */
    @NotBlank
    var namespaceCode: String? = null

    /**
     * 工作流编码
     */
    @NotBlank
    var code: String? = null

    /**
     * 工作流名称
     */
    @NotBlank
    var name: String? = null
}