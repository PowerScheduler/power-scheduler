package tech.powerscheduler.server.infrastructure.persistence.repository

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.workflow.Workflow
import tech.powerscheduler.server.domain.workflow.WorkflowId
import tech.powerscheduler.server.domain.workflow.WorkflowQuery
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
import tech.powerscheduler.server.infrastructure.persistence.model.AppGroupEntity
import tech.powerscheduler.server.infrastructure.persistence.model.NamespaceEntity
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowEntity
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.WorkflowJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toDomainPage
import tech.powerscheduler.server.infrastructure.utils.toEntity
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/22
 */
@Repository
class WorkflowRepositoryImpl(
    private val workflowJpaRepository: WorkflowJpaRepository
) : WorkflowRepository {

    override fun listAssignableIds(pageQuery: PageQuery): Page<WorkflowId> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1,
            pageQuery.pageSize,
            Sort.by(WorkflowEntity::id.name).descending()
        )
        val specification = Specification<WorkflowEntity> { root, _, criteriaBuilder ->
            val notAssigned = criteriaBuilder.isNull(root.get<Boolean>(WorkflowEntity::schedulerAddress.name))
            criteriaBuilder.and(notAssigned)
        }
        val page = workflowJpaRepository.findAll(specification, pageable)
        return page.map { WorkflowId(it.id!!) }.toDomainPage()
    }

    override fun listAllIds(pageQuery: PageQuery): Page<WorkflowId> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1,
            pageQuery.pageSize,
            Sort.by(WorkflowEntity::id.name).descending()
        )
        val page = workflowJpaRepository.findAll(pageable)
        return page.map { WorkflowId(it.id!!) }.toDomainPage()
    }

    override fun lockById(workflowId: WorkflowId): Workflow? {
        val entity = workflowJpaRepository.findByIdForUpdate(workflowId.value)
        return entity?.toDomainModel()
    }

    override fun findById(workflowId: WorkflowId): Workflow? {
        val entity = workflowJpaRepository.findByIdOrNull(workflowId.value)
        return entity?.toDomainModel()
    }

    override fun pageQuery(query: WorkflowQuery): Page<Workflow> {
        val pageable = PageRequest.of(
            query.pageNo - 1,
            query.pageSize,
            Sort.by(WorkflowEntity::id.name).descending()
        )
        val specification = Specification<WorkflowEntity> { root, _, criteriaBuilder ->
            val joinAppGroup = root.join<WorkflowEntity, AppGroupEntity>(
                WorkflowEntity::appGroupEntity.name,
                JoinType.INNER,
            )
            val joinNamespace = joinAppGroup.join<AppGroupEntity, NamespaceEntity>(
                AppGroupEntity::namespaceEntity.name,
                JoinType.INNER,
            )
            val namespaceCodeEqual = query.namespaceCode.let {
                criteriaBuilder.equal(joinNamespace.get<String>(NamespaceEntity::code.name), it)
            }
            val appCodeEqual = query.appCode.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.equal(joinAppGroup.get<String>(AppGroupEntity::code.name), it)
            }
            val nameLike = query.name.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.like(root.get(WorkflowEntity::name.name), "%$it%")
            }
            val predicates = listOfNotNull(namespaceCodeEqual, appCodeEqual, nameLike)
            criteriaBuilder.and(*predicates.toTypedArray())
        }
        val page = workflowJpaRepository.findAll(specification, pageable)
        return page.map { it.toDomainModel() }.toDomainPage()
    }

    override fun listIdsByEnabledAndSchedulerAddress(
        enabled: Boolean?,
        schedulerAddress: String,
        pageQuery: PageQuery
    ): Page<WorkflowId> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1,
            pageQuery.pageSize,
            Sort.by(WorkflowEntity::id.name).descending()
        )
        val page = workflowJpaRepository.listIdsByEnabledAndSchedulerAddress(
            enabled,
            schedulerAddress,
            pageable,
        )
        return page.map { WorkflowId(it) }.toDomainPage()
    }

    override fun findSchedulableByIds(
        ids: List<WorkflowId>,
        baseTime: LocalDateTime
    ): List<Workflow> {
        val nextScheduleAt = baseTime.plusSeconds(10)
        val specification = Specification<WorkflowEntity> { root, _, cb ->
            val idIn = root.get<Long>(WorkflowEntity::id.name).`in`(ids.map { it.value })
            val isEnabled = cb.equal(root.get<Boolean>(WorkflowEntity::enabled.name), true)
            val nextScheduleAtGreatEquals = cb.lessThanOrEqualTo(
                root.get(WorkflowEntity::nextScheduleAt.name),
                nextScheduleAt
            )
            cb.and(
                isEnabled,
                idIn,
                nextScheduleAtGreatEquals,
            )
        }
        val list = workflowJpaRepository.findAll(specification)
        return list.map { it.toDomainModel() }
    }

    override fun save(workflow: Workflow): WorkflowId {
        val entity = workflow.toEntity()
        workflowJpaRepository.save(entity)
        workflow.id = WorkflowId(entity.id!!)
        return workflow.id!!
    }

    override fun deleteById(workflowId: WorkflowId) {
        workflowJpaRepository.deleteById(workflowId.value)
    }

    override fun clearSchedulerByAddress(address: String) {
        workflowJpaRepository.clearSchedulerByAddress(address)
    }

}