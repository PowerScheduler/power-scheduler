package tech.powerscheduler.server.infrastructure.persistence.repository

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.server.domain.workflow.Workflow
import tech.powerscheduler.server.domain.workflow.WorkflowNode
import tech.powerscheduler.server.domain.workflow.WorkflowNodeId
import tech.powerscheduler.server.domain.workflow.WorkflowNodeRepository
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.WorkflowNodeJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toEntity

/**
 * @author grayrat
 * @since 2025/6/22
 */
@Repository
class WorkflowNodeRepositoryImpl(
    private val workflowNodeJpaRepository: WorkflowNodeJpaRepository
) : WorkflowNodeRepository {

    override fun findById(id: WorkflowNodeId): WorkflowNode? {
        val entity = workflowNodeJpaRepository.findByIdOrNull(id.value)
        return entity?.toDomainModel()
    }

    override fun findAllByIds(ids: Iterable<WorkflowNodeId>): List<WorkflowNode> {
        val entities = workflowNodeJpaRepository.findAllById(ids.map { it.value })
        return entities.map { it.toDomainModel() }
    }

    override fun findAllByWorkflow(workflow: Workflow): List<WorkflowNode> {
        val workflowEntity = workflow.toEntity()
        val entities = workflowNodeJpaRepository.findAllByWorkflowEntity(workflowEntity)
        return entities.map { it.toDomainModel() }
    }

    override fun save(workflowNode: WorkflowNode): WorkflowNodeId {
        val entity = workflowNode.toEntity()
        workflowNodeJpaRepository.save(entity)
        return WorkflowNodeId(entity.id!!)
    }

    override fun saveAll(workflowNodes: Iterable<WorkflowNode>) {
        val entities = workflowNodes.map { it.toEntity() }
        workflowNodeJpaRepository.saveAll(entities)
    }

    override fun deleteById(id: WorkflowNodeId) {
        workflowNodeJpaRepository.deleteById(id.value)
    }

    override fun deleteByIds(ids: Iterable<WorkflowNodeId>) {
        workflowNodeJpaRepository.deleteAllByIdInBatch(ids.map { it.value })
    }

}