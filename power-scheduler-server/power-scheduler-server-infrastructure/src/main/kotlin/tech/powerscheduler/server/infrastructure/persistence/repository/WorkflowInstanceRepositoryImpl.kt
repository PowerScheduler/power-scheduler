package tech.powerscheduler.server.infrastructure.persistence.repository

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.workflow.*
import tech.powerscheduler.server.infrastructure.persistence.model.AppGroupEntity
import tech.powerscheduler.server.infrastructure.persistence.model.JobInstanceEntity
import tech.powerscheduler.server.infrastructure.persistence.model.NamespaceEntity
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowInstanceEntity
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.WorkflowInstanceJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toDomainPage
import tech.powerscheduler.server.infrastructure.utils.toEntity

/**
 * @author grayrat
 * @since 2025/6/22
 */
@Repository
class WorkflowInstanceRepositoryImpl(
    private val workflowInstanceJpaRepository: WorkflowInstanceJpaRepository
) : WorkflowInstanceRepository {

    override fun countByWorkflowIdAndStatus(
        workflowIds: List<WorkflowId>,
        statuses: Set<WorkflowStatusEnum>,
    ): Map<WorkflowId, Long> {
        val group = workflowInstanceJpaRepository.countGroupByWorkflowIdAndStatus(
            workflowIds = workflowIds.map { it.value },
            statuses = statuses,
        )
        return group.associate { WorkflowId(it[0] as Long) to it[1] as Long }
    }

    override fun lockById(workflowInstanceId: WorkflowInstanceId): WorkflowInstance? {
        val entity = workflowInstanceJpaRepository.findByIdForUpdate(workflowInstanceId.value)
        return entity?.toDomainModel()
    }

    override fun lockByCode(code: String): WorkflowInstance? {
        val entity = workflowInstanceJpaRepository.findByCodeForUpdate(code)
        return entity?.toDomainModel()
    }

    override fun pageQuery(query: WorkflowInstanceQuery): Page<WorkflowInstance> {
        val pageable = PageRequest.of(
            query.pageNo - 1,
            query.pageSize,
            Sort.by(JobInstanceEntity::id.name).descending()
        )
        val specification = Specification<WorkflowInstanceEntity> { root, _, criteriaBuilder ->
            val appGroupJoin = root.join<WorkflowInstanceEntity, AppGroupEntity>(
                WorkflowInstanceEntity::appGroupEntity.name, JoinType.INNER
            )
            val namespaceJoin = appGroupJoin.join<AppGroupEntity, NamespaceEntity>(
                AppGroupEntity::namespaceEntity.name, JoinType.INNER
            )
            val namespaceCodeEquals = criteriaBuilder.equal(
                namespaceJoin.get<String>(NamespaceEntity::code.name), query.namespaceCode
            )
            val appCodeEqual = query.appCode.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.equal(appGroupJoin.get<String>(AppGroupEntity::code.name), it)
            }
            val workflowIdEqual = query.workflowId?.let {
                criteriaBuilder.equal(root.get<Long>(WorkflowInstanceEntity::workflowId.name), it)
            }
            val workflowInstanceIdEqual = query.workflowInstanceId?.let {
                criteriaBuilder.equal(root.get<Long>(WorkflowInstanceEntity::id.name), it)
            }
            val statusEqual = query.status?.let {
                criteriaBuilder.equal(root.get<WorkflowStatusEnum>(WorkflowInstanceEntity::status.name), it)
            }
            val startAtBetween = query.startAtRange?.let {
                criteriaBuilder.between(root.get(JobInstanceEntity::startAt.name), it[0], it[1])
            }
            val endAtBetween = query.endAtRange?.let {
                criteriaBuilder.between(root.get(JobInstanceEntity::endAt.name), it[0], it[1])
            }
            val predicates = listOfNotNull(
                namespaceCodeEquals, appCodeEqual,
                workflowIdEqual, workflowInstanceIdEqual, statusEqual,
                startAtBetween, endAtBetween
            )
            criteriaBuilder.and(*predicates.toTypedArray())
        }
        val page = workflowInstanceJpaRepository.findAll(specification, pageable)
        return page.map { it.toDomainModel() }.toDomainPage()
    }

    override fun findById(workflowInstanceId: WorkflowInstanceId): WorkflowInstance? {
        val entity = workflowInstanceJpaRepository.findByIdOrNull(workflowInstanceId.value)
        return entity?.toDomainModel()
    }

    override fun save(workflowInstance: WorkflowInstance): WorkflowInstanceId {
        val entity = workflowInstance.toEntity()
        workflowInstanceJpaRepository.save(entity)
        return WorkflowInstanceId(entity.id!!)
    }
}