package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.common.PageQuery
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/9
 */
class WorkflowInstanceQuery : PageQuery() {
    /**
     * 命名空间编码
     */
    var namespaceCode: String? = null

    /**
     * 应用编码
     */
    var appCode: String? = null

    /**
     * 工作流id
     */
    var workflowId: Long? = null

    /**
     * 工作流实例id
     */
    var workflowInstanceId: Long? = null

    /**
     * 状态
     */
    var status: WorkflowStatusEnum? = null

    /**
     * 开始时间区间
     */
    var startAtRange: Array<LocalDateTime>? = null

    /**
     * 结束时间区间
     */
    var endAtRange: Array<LocalDateTime>? = null
}