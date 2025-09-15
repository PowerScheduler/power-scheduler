package tech.powerscheduler.server.domain.workflow

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowNodeInstanceRepository {

    fun findById(workflowNodeInstanceId: WorkflowNodeInstanceId): WorkflowNodeInstance?

    fun save(workflowNodeInstance: WorkflowNodeInstance): WorkflowNodeInstanceId

    fun saveAll(workflowNodeInstances: Iterable<WorkflowNodeInstance>)
}