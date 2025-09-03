package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowInstanceRepository {

    fun countByWorkflowIdAndStatus(
        workflowIds: List<WorkflowId>,
        statuses: Set<WorkflowStatusEnum>,
    ): Map<WorkflowId, Long>

    fun findAllByWorkflowIdAndStatus(
        workflowId: WorkflowId,
        statuses: Set<WorkflowStatusEnum>,
        pageQuery: PageQuery,
    ): Page<WorkflowInstance>

    fun lockById(workflowInstanceId: WorkflowInstanceId): WorkflowInstance?

    fun lockByCode(code: String): WorkflowInstance?

    fun pageQuery(query: WorkflowInstanceQuery): Page<WorkflowInstance>

    fun findById(workflowInstanceId: WorkflowInstanceId): WorkflowInstance?

    fun save(workflowInstance: WorkflowInstance): WorkflowInstanceId

    fun deleteAll(workflowInstances: Iterable<WorkflowInstance>)

    fun findAllByWorkflowIdAndStatusAndEndAtBefore(
        workflowId: WorkflowId,
        statuses: Set<WorkflowStatusEnum>,
        endAt: LocalDateTime,
        pageQuery: PageQuery
    ): Page<WorkflowInstance>
}