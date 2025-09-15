package tech.powerscheduler.server.infrastructure.persistence.repository

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstance
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstanceId
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstanceRepository
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.WorkflowNodeInstanceJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toEntity

/**
 * @author grayrat
 * @since 2025/6/22
 */
@Repository
class WorkflowNodeInstanceRepositoryImpl(
    private val workflowNodeInstanceJpaRepository: WorkflowNodeInstanceJpaRepository
) : WorkflowNodeInstanceRepository {

    override fun findById(workflowNodeInstanceId: WorkflowNodeInstanceId): WorkflowNodeInstance? {
        val entity = workflowNodeInstanceJpaRepository.findByIdOrNull(workflowNodeInstanceId.value)
        return entity?.toDomainModel()
    }

    override fun save(workflowNodeInstance: WorkflowNodeInstance): WorkflowNodeInstanceId {
        val entity = workflowNodeInstance.toEntity()
        workflowNodeInstanceJpaRepository.save(entity)
        return WorkflowNodeInstanceId(entity.id!!)
    }

    override fun saveAll(workflowNodeInstances: Iterable<WorkflowNodeInstance>) {
        val entities = workflowNodeInstances.map { it.toEntity() }
        workflowNodeInstanceJpaRepository.saveAll(entities)
    }
}