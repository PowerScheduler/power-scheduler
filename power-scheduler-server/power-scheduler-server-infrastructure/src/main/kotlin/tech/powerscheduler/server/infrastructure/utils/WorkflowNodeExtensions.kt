package tech.powerscheduler.server.infrastructure.utils

import tech.powerscheduler.server.domain.workflow.WorkflowNode
import tech.powerscheduler.server.domain.workflow.WorkflowNodeId
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowNodeEntity

/**
 * @author grayrat
 * @since 2025/7/9
 */
fun WorkflowNode.toEntity(): WorkflowNodeEntity {
    return WorkflowNodeEntity().also {
        it.id = this.id?.value
        it.code = this.code
        it.name = this.name
        it.description = this.description
        it.jobType = this.jobType
        it.processor = this.processor
        it.executeMode = this.executeMode
        it.executeParams = this.executeParams
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.maxAttemptCnt = this.maxAttemptCnt
        it.attemptInterval = this.attemptInterval
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
    }
}

fun WorkflowNodeEntity.toDomainModel(): WorkflowNode {
    return WorkflowNode().also {
        it.id = WorkflowNodeId(this.id!!)
        it.code = this.code
        it.name = this.name
        it.description = this.description
        it.jobType = this.jobType
        it.processor = this.processor
        it.executeMode = this.executeMode
        it.executeParams = this.executeParams
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.maxAttemptCnt = this.maxAttemptCnt
        it.attemptInterval = this.attemptInterval
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
    }
}