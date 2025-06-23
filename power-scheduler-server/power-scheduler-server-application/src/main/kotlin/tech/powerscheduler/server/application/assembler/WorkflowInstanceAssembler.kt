package tech.powerscheduler.server.application.assembler

import org.springframework.stereotype.Component
import tech.powerscheduler.server.application.dto.request.WorkflowInstanceQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.WorkflowInstanceDetailResponseDTO
import tech.powerscheduler.server.application.dto.response.WorkflowInstanceQueryResponseDTO
import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.workflow.WorkflowInstance
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceQuery

/**
 * @author grayrat
 * @since 2025/7/9
 */
@Component
class WorkflowInstanceAssembler {

    fun toDomainQuery(param: WorkflowInstanceQueryRequestDTO): WorkflowInstanceQuery {
        return WorkflowInstanceQuery().apply {
            this.pageNo = param.pageNo
            this.pageSize = param.pageSize
            this.namespaceCode = param.namespaceCode
            this.appCode = param.appCode
            this.workflowId = param.workflowId
            this.workflowInstanceId = param.workflowInstanceId
            this.status = param.status
            this.startAtRange = param.startAtRange
            this.endAtRange = param.endAtRange
        }
    }

    fun toWorkflowInstanceQueryResponseDTO(workflowInstance: WorkflowInstance): WorkflowInstanceQueryResponseDTO {
        return WorkflowInstanceQueryResponseDTO().apply {
            val appGroup = workflowInstance.appGroup!!
            this.appCode = appGroup.code
            this.appName = appGroup.name
            this.id = workflowInstance.id!!.value
            this.workflowId = workflowInstance.workflowId!!.value
            this.name = workflowInstance.name
            this.code = workflowInstance.code
            this.status = workflowInstance.status.toDTO()
            this.startAt = workflowInstance.startAt
            this.endAt = workflowInstance.endAt
            this.dataTime = workflowInstance.dataTime
        }
    }

    fun toWorkflowInstanceDetailResponseDTO(workflowInstance: WorkflowInstance): WorkflowInstanceDetailResponseDTO {
        return WorkflowInstanceDetailResponseDTO().apply {
            val appGroup = workflowInstance.appGroup!!
            this.appCode = appGroup.code
            this.appName = appGroup.name
            this.id = workflowInstance.id!!.value
            this.workflowId = workflowInstance.workflowId!!.value
            this.name = workflowInstance.name
            this.code = workflowInstance.code
            this.status = workflowInstance.status
            this.startAt = workflowInstance.startAt
            this.endAt = workflowInstance.endAt
            this.dataTime = workflowInstance.dataTime
            this.graphData = JSON.writeValueAsString(workflowInstance.graphData!!)
        }
    }

}