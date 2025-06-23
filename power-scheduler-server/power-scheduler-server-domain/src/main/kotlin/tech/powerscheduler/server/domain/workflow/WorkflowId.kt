package tech.powerscheduler.server.domain.workflow

import tech.powerscheduler.server.domain.job.SourceId

/**
 * @author grayrat
 * @since 2025/6/21
 */
@JvmInline
value class WorkflowId(val value: Long) {
    fun toSourceId() = SourceId(value)
}