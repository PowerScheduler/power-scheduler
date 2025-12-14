package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotBlank

/**
 * 工作流分组编辑请求参数
 *
 * @author grayrat
 * @since 2025/12/7
 */
class WorkflowGroupEditRequestDTO {
    /**
     * 工作流分组名称
     */
    @NotBlank
    var name: String? = null
}