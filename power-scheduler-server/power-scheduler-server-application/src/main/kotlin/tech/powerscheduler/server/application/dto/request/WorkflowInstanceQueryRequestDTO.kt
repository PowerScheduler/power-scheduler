package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotBlank
import tech.powerscheduler.common.dto.request.PageQueryRequestDTO
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/9
 */
class WorkflowInstanceQueryRequestDTO : PageQueryRequestDTO() {
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