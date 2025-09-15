package tech.powerscheduler.server.application.assembler

import tech.powerscheduler.server.application.dto.response.WorkflowNodeInstanceDetailResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstance

class WorkflowNodeInstanceAssembler {

    fun toWorkflowNodeInstanceDetailResponseDTO(
        workflowNodeInstance: WorkflowNodeInstance
    ): WorkflowNodeInstanceDetailResponseDTO {
        return WorkflowNodeInstanceDetailResponseDTO().apply {
            this.id = workflowNodeInstance.id?.value
            this.parentIds = workflowNodeInstance.parents.mapNotNull { it.id?.value }
            this.childrenIds = workflowNodeInstance.children.mapNotNull { it.id?.value }
            this.nodeCode = workflowNodeInstance.nodeCode
            this.nodeInstanceCode = workflowNodeInstance.nodeInstanceCode
            this.name = workflowNodeInstance.name
            this.jobType = workflowNodeInstance.jobType.toDTO()
            this.processor = workflowNodeInstance.processor
            this.status = workflowNodeInstance.status.toDTO()
            this.executeMode = workflowNodeInstance.executeMode.toDTO()
            this.executeParams = workflowNodeInstance.executeParams
            this.scriptType = workflowNodeInstance.scriptType.toDTO()
            this.scriptCode = workflowNodeInstance.scriptCode
            this.dataTime = workflowNodeInstance.dataTime
            this.startAt = workflowNodeInstance.startAt
            this.endAt = workflowNodeInstance.endAt
            this.workerAddress = workflowNodeInstance.workerAddress
            this.maxAttemptCnt = workflowNodeInstance.maxAttemptCnt
            this.attemptInterval = workflowNodeInstance.attemptInterval
            this.taskMaxAttemptCnt = workflowNodeInstance.taskMaxAttemptCnt
            this.taskAttemptInterval = workflowNodeInstance.taskAttemptInterval
            this.priority = workflowNodeInstance.priority
        }
    }
}