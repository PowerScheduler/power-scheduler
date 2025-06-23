package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowRepository {

    fun listAssignableIds(pageQuery: PageQuery): Page<WorkflowId>

    fun listAllIds(pageQuery: PageQuery): Page<WorkflowId>

    fun lockById(workflowId: WorkflowId): Workflow?

    fun findById(workflowId: WorkflowId): Workflow?

    fun pageQuery(query: WorkflowQuery): Page<Workflow>

    fun findSchedulableByIds(ids: List<WorkflowId>, baseTime: LocalDateTime): List<Workflow>

    fun listIdsByEnabledAndSchedulerAddress(
        enabled: Boolean?,
        schedulerAddress: String,
        pageQuery: PageQuery
    ): Page<WorkflowId>

    fun save(workflow: Workflow): WorkflowId

    fun deleteById(workflowId: WorkflowId)

    fun clearSchedulerByAddress(address: String)

}