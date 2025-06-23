package tech.powerscheduler.server.domain.workflow

data class WorkflowNodeInstanceStatusChangeEvent(
    val workflowInstanceId: Long,
) {
    companion object {
        fun create(workflowInstanceId: WorkflowInstanceId): WorkflowNodeInstanceStatusChangeEvent {
            return WorkflowNodeInstanceStatusChangeEvent(workflowInstanceId.value)
        }
    }
}