package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import tech.powerscheduler.server.application.assembler.WorkflowNodeAssembler
import tech.powerscheduler.server.domain.workflow.WorkflowNodeRepository
import tech.powerscheduler.server.domain.workflow.WorkflowRepository

/**
 * @author grayrat
 * @since 2025/6/23
 */
@Service
class WorkflowNodeService(
    private val workflowNodeAssembler: WorkflowNodeAssembler,
    private val workflowRepository: WorkflowRepository,
    private val workflowNodeRepository: WorkflowNodeRepository,
) {


}