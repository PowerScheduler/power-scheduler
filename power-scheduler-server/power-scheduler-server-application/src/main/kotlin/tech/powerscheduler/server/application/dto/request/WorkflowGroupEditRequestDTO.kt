package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 工作流分组编辑请求参数
 *
 * @author grayrat
 * @since 2025/12/7
 */
class WorkflowGroupEditRequestDTO {
    /**
     * 应用id
     */
    @NotNull
    var id: Long? = null

    /**
     * 应用分组名称
     */
    @NotBlank
    var name: String? = null
}