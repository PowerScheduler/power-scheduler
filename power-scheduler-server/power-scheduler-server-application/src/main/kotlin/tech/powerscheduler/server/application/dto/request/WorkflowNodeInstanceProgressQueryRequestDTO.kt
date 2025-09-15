package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotNull
import tech.powerscheduler.common.dto.request.PageQueryRequestDTO

/**
 * 工作流节点的任务进度查询参数
 *
 * @author grayrat
 * @since 2025/9/15
 */
class WorkflowNodeInstanceProgressQueryRequestDTO  : PageQueryRequestDTO() {
    /**
     * 任务实例ID
     */
    @NotNull
    var workflowNodeInstanceId: Long? = null
}