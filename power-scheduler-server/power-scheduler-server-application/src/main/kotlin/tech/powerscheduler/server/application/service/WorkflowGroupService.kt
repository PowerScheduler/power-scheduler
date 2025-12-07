package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.WorkflowGroupAssembler
import tech.powerscheduler.server.application.context.UserContext
import tech.powerscheduler.server.application.dto.request.WorkflowGroupAddRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowGroupEditRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowGroupQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.WorkflowGroupQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.namespace.NamespaceRepository
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroupId
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroupRepository

/**
 * @author grayrat
 * @since 2025/12/7
 */
@Service
class WorkflowGroupService(
    private val namespaceRepository: NamespaceRepository,
    private val workflowGroupRepository: WorkflowGroupRepository,
    private val workflowGroupAssembler: WorkflowGroupAssembler,
) {
    fun list(param: WorkflowGroupQueryRequestDTO): PageDTO<WorkflowGroupQueryResponseDTO> {
        val query = workflowGroupAssembler.toDomainQuery(param)
        val workflowGroupPage = workflowGroupRepository.pageQuery(query)
        return workflowGroupPage.toDTO().map { workflowGroupAssembler.toWorkflowGroupQueryResponseDTO(it) }
    }

    fun add(param: WorkflowGroupAddRequestDTO, userContext: UserContext): Long {
        val namespace = namespaceRepository.findByCode(param.namespaceCode!!)
            ?: throw BizException("A namespace with code ${param.namespaceCode} not found")
        val workflowGroupToSave = workflowGroupAssembler.toDomainModel4AddRequest(
            param = param,
            namespace = namespace,
            userContext = userContext
        )
        val workflowGroupId = workflowGroupRepository.save(workflowGroupToSave)
        return workflowGroupId.value
    }

    fun edit(param: WorkflowGroupEditRequestDTO, userContext: UserContext) {
        val workflowGroup = workflowGroupRepository.findById(WorkflowGroupId(param.id!!))
            ?: throw BizException("A workflowGroup with id ${param.id} not found")
        val workflowGroupToSave = workflowGroupAssembler.toDomainModel4EditRequest(
            model = workflowGroup,
            param = param,
            userContext = userContext
        )
        workflowGroupRepository.save(workflowGroupToSave)
    }
}