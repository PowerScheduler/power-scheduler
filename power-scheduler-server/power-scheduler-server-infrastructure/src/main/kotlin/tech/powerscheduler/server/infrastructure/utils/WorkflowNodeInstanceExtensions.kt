package tech.powerscheduler.server.infrastructure.utils

import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstance
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstanceId
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowNodeInstanceEntity

/**
 * @author grayrat
 * @since 2025/7/9
 */
fun WorkflowNodeInstance.toEntity(): WorkflowNodeInstanceEntity {
    return WorkflowNodeInstanceEntity().also {
        it.id = this.id?.value
        it.name = this.name
        it.nodeCode = this.nodeCode
        it.nodeInstanceCode = this.nodeInstanceCode
        it.jobType = this.jobType
        it.status = this.status
        it.processor = this.processor
        it.executeMode = this.executeMode
        it.executeParams = this.executeParams
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.dataTime = this.dataTime
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.workerAddress = this.workerAddress
        it.maxAttemptCnt = this.maxAttemptCnt
        it.attemptInterval = this.attemptInterval
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
    }
}

fun WorkflowNodeInstanceEntity.toDomainModel(): WorkflowNodeInstance {
    return WorkflowNodeInstance().also {
        it.id = WorkflowNodeInstanceId(this.id!!)
        it.name = this.name
        it.nodeCode = this.nodeCode
        it.nodeInstanceCode = this.nodeInstanceCode
        it.jobType = this.jobType
        it.status = this.status
        it.processor = this.processor
        it.executeMode = this.executeMode
        it.executeParams = this.executeParams
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.dataTime = this.dataTime
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.workerAddress = this.workerAddress
        it.maxAttemptCnt = this.maxAttemptCnt
        it.attemptInterval = this.attemptInterval
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
    }
}