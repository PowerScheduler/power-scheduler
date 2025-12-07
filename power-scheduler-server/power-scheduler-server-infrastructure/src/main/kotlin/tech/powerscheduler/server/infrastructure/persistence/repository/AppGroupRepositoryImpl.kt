package tech.powerscheduler.server.infrastructure.persistence.repository

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.server.domain.appgroup.AppGroup
import tech.powerscheduler.server.domain.appgroup.AppGroupId
import tech.powerscheduler.server.domain.appgroup.AppGroupQuery
import tech.powerscheduler.server.domain.appgroup.AppGroupRepository
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.namespace.Namespace
import tech.powerscheduler.server.infrastructure.persistence.model.AppGroupEntity
import tech.powerscheduler.server.infrastructure.persistence.model.NamespaceEntity
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.AppGroupJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toDomainPage
import tech.powerscheduler.server.infrastructure.utils.toEntity

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Repository
class AppGroupRepositoryImpl(
    private val appGroupJpaRepository: AppGroupJpaRepository
) : AppGroupRepository {

    override fun pageQuery(query: AppGroupQuery): Page<AppGroup> {
        val pageable = PageRequest.of(
            query.pageNo - 1, query.pageSize, Sort.by(AppGroupEntity::id.name).descending()
        )
        val specification = Specification<AppGroupEntity> { root, _, criteriaBuilder ->
            val join = root.join<AppGroupEntity, NamespaceEntity>(
                AppGroupEntity::namespaceEntity.name, JoinType.INNER
            )
            val namespaceCodeEqual = criteriaBuilder.equal(
                join.get<String>(NamespaceEntity::code.name),
                query.namespaceCode
            )
            val codeLike = query.code.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.like(root.get(AppGroupEntity::code.name), "%$it%")
            }
            val nameLike = query.name.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.like(root.get(AppGroupEntity::name.name), "%$it%")
            }
            val predicates = listOfNotNull(namespaceCodeEqual, codeLike, nameLike)
            criteriaBuilder.and(*predicates.toTypedArray())
        }
        val page = appGroupJpaRepository.findAll(specification, pageable)
        return page.map { it.toDomainModel() }.toDomainPage()
    }

    override fun findById(appGroupId: AppGroupId): AppGroup? {
        val entity = appGroupJpaRepository.findByIdOrNull(appGroupId.value)
        return entity?.toDomainModel()
    }

    override fun findByCode(namespace: Namespace, code: String): AppGroup? {
        val namespaceEntity = namespace.toEntity()
        val entity = appGroupJpaRepository.findByNamespaceEntityAndCode(
            namespaceEntity = namespaceEntity,
            code = code,
        )
        return entity?.toDomainModel()
    }

    override fun save(model: AppGroup): AppGroup {
        val entity = model.toEntity()
        appGroupJpaRepository.save(entity)
        return entity.toDomainModel()
    }

    override fun findByCodes(appCodes: Collection<String>): List<AppGroup> {
        val entities = appGroupJpaRepository.findAllByCodeIn(appCodes)
        return entities.map { it.toDomainModel() }
    }
}