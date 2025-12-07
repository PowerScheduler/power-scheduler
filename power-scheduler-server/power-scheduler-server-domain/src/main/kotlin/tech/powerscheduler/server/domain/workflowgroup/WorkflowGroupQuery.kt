package tech.powerscheduler.server.domain.workflowgroup

import tech.powerscheduler.common.dto.request.PageQueryRequestDTO

/**
 * @author grayrat
 * @since 2025/12/7
 */
class WorkflowGroupQuery : PageQueryRequestDTO() {
    /**
     * 命名空间编码
     */
    var namespaceCode: String? = null

    /**
     * 应用编码
     */
    var code: String? = null

    /**
     * 应用分组名称
     */
    var name: String? = null
}