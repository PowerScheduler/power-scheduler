package tech.powerscheduler.server.domain.job

import tech.powerscheduler.server.domain.workflow.WorkflowInstanceId

/**
 * @author grayrat
 * @since 2025/7/10
 */
@JvmInline
value class SourceId(val value: Long) {
    fun toJobId(): JobId {
        return JobId(value)
    }

    fun toWorkflowInstanceId(): WorkflowInstanceId {
        return WorkflowInstanceId(value)
    }
}