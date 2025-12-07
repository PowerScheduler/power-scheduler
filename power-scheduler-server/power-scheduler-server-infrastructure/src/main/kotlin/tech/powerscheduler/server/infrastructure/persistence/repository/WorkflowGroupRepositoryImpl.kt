package tech.powerscheduler.server.infrastructure.persistence.repository

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.namespace.Namespace
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroup
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroupId
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroupQuery
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroupRepository
import tech.powerscheduler.server.infrastructure.persistence.model.NamespaceEntity
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowGroupEntity
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.WorkflowGroupJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toDomainPage
import tech.powerscheduler.server.infrastructure.utils.toEntity

/**
 * @author grayrat
 * @since 2025/12/7
 */
@Repository
class WorkflowGroupRepositoryImpl(
    private val workflowGroupJpaRepository: WorkflowGroupJpaRepository
) : WorkflowGroupRepository {

    override fun pageQuery(query: WorkflowGroupQuery): Page<WorkflowGroup> {
        val pageable = PageRequest.of(
            query.pageNo - 1, query.pageSize, Sort.by(WorkflowGroupEntity::id.name).descending()
        )
        val specification = Specification<WorkflowGroupEntity> { root, _, criteriaBuilder ->
            val join = root.join<WorkflowGroupEntity, NamespaceEntity>(
                WorkflowGroupEntity::namespaceEntity.name, JoinType.INNER
            )
            val namespaceCodeEqual = criteriaBuilder.equal(
                join.get<String>(NamespaceEntity::code.name),
                query.namespaceCode
            )
            val codeLike = query.code.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.like(root.get(WorkflowGroupEntity::code.name), "%$it%")
            }
            val nameLike = query.name.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.like(root.get(WorkflowGroupEntity::name.name), "%$it%")
            }
            val predicates = listOfNotNull(namespaceCodeEqual, codeLike, nameLike)
            criteriaBuilder.and(*predicates.toTypedArray())
        }
        val page = workflowGroupJpaRepository.findAll(specification, pageable)
        return page.map { it.toDomainModel() }.toDomainPage()
    }

    override fun findById(workflowGroupId: WorkflowGroupId): WorkflowGroup? {
        val entity = workflowGroupJpaRepository.findByIdOrNull(workflowGroupId.value)
        return entity?.toDomainModel()
    }

    override fun findByNamespaceAndCode(
        namespace: Namespace,
        code: String
    ): WorkflowGroup? {
        val namespaceEntity = namespace.toEntity()
        val workflowGroup = workflowGroupJpaRepository.findByNamespaceEntityAndCode(namespaceEntity, code)
        return workflowGroup?.toDomainModel()
    }

    override fun save(workflowGroup: WorkflowGroup): WorkflowGroupId {
        val entity = workflowGroup.toEntity()
        workflowGroupJpaRepository.save(entity)
        return WorkflowGroupId(entity.id!!)
    }

}