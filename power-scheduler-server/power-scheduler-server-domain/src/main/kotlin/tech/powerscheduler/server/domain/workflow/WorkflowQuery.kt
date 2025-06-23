package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.server.domain.common.PageQuery

/**
 * @author grayrat
 * @since 2025/6/23
 */
class WorkflowQuery : PageQuery() {
    /**
     * 命名空间编码
     */
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