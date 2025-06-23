package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotBlank
import tech.powerscheduler.common.dto.request.PageQueryRequestDTO

/**
 * @author grayrat
 * @since 2025/6/23
 */
class WorkflowQueryRequestDTO : PageQueryRequestDTO() {
    /**
     * 命名空间编码
     */
    @NotBlank
    var namespaceCode: String? = null

    /**
     * 应用编码
     */
    var appCode: String? = null

    /**
     * 工作流名称
     */
    var name: String? = null
}