package tech.powerscheduler.server.domain.workflow

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowNodeRepository {

    fun findById(id: WorkflowNodeId): WorkflowNode?

    fun findAllByIds(ids: Iterable<WorkflowNodeId>): List<WorkflowNode>

    fun findAllByWorkflow(workflow: Workflow): List<WorkflowNode>

    fun save(workflowNode: WorkflowNode): WorkflowNodeId

    fun saveAll(workflowNodes: Iterable<WorkflowNode>)

    fun deleteById(id: WorkflowNodeId)

    fun deleteByIds(ids: Iterable<WorkflowNodeId>)

}