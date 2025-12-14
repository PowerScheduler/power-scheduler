package tech.powerscheduler.server.application.assembler

import org.springframework.stereotype.Component
import tech.powerscheduler.server.application.context.UserContext
import tech.powerscheduler.server.application.dto.request.WorkflowGroupAddRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowGroupEditRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowGroupQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.WorkflowGroupQueryResponseDTO
import tech.powerscheduler.server.domain.namespace.Namespace
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroup
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroupQuery
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/12/7
 */
@Component
class WorkflowGroupAssembler {

    fun toDomainQuery(param: WorkflowGroupQueryRequestDTO): WorkflowGroupQuery {
        return WorkflowGroupQuery().apply {
            this.namespaceCode = param.namespaceCode
            this.code = param.workflowGroupCode
            this.name = param.workflowGroupName
        }
    }

    fun toWorkflowGroupQueryResponseDTO(model: WorkflowGroup): WorkflowGroupQueryResponseDTO {
        return WorkflowGroupQueryResponseDTO().apply {
            this.id = model.id!!.value
            this.namespaceCode = model.namespace?.code
            this.code = model.code
            this.name = model.name
            this.createdBy = model.createdBy
            this.createdAt = model.createdAt
        }
    }

    fun toDomainModel4AddRequest(
        param: WorkflowGroupAddRequestDTO,
        namespace: Namespace,
        userContext: UserContext,
    ): WorkflowGroup {
        return WorkflowGroup().apply {
            this.namespace = namespace
            this.code = param.code
            this.name = param.name
            this.createdBy = userContext.userNo
            this.createdAt = LocalDateTime.now()
            this.updatedBy = userContext.userNo
            this.updatedAt = LocalDateTime.now()
        }
    }

    fun toDomainModel4EditRequest(
        model: WorkflowGroup,
        param: WorkflowGroupEditRequestDTO,
        userContext: UserContext,
    ): WorkflowGroup {
        return WorkflowGroup().apply {
            this.namespace = model.namespace
            this.id = model.id
            this.code = model.code
            this.name = param.name
            this.createdBy = model.createdBy
            this.createdAt = model.createdAt
            this.updatedBy = userContext.userNo
            this.updatedAt = LocalDateTime.now()
        }
    }
}