package tech.powerscheduler.server.infrastructure.persistence.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.domainevent.*
import tech.powerscheduler.server.domain.job.JobInstanceId
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.DomainEventJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toDomainPage
import tech.powerscheduler.server.infrastructure.utils.toEntity

/**
 * @author grayrat
 * @since 2025/6/8
 */
@Repository
class DomainEventRepositoryImpl(
    private val domainEventJpaRepository: DomainEventJpaRepository
) : DomainEventRepository {

    override fun findPendingList(
        eventType: DomainEventTypeEnum,
        pageQuery: PageQuery,
    ): Page<DomainEvent> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1,
            pageQuery.pageSize,
            Sort.by(DomainEvent::id.name).descending()
        )
        val entities = domainEventJpaRepository.findAllByEventTypeAndEventStatus(
            eventStatus = DomainEventStatusEnum.PENDING,
            eventType = eventType,
            pageable = pageable,
        )
        return entities.map { it.toDomainModel() }.toDomainPage()
    }

    override fun save(domainEvent: DomainEvent): DomainEventId {
        val entity = domainEvent.toEntity()
        domainEventJpaRepository.save(entity)
        return DomainEventId(entity.id!!)
    }

    override fun deleteByIds(ids: Iterable<DomainEventId>) {
        val ids = ids.map { it.value }
        domainEventJpaRepository.deleteAllByIdInBatch(ids)
    }

    override fun deleteByEventStatus(status: DomainEventStatusEnum) {
        domainEventJpaRepository.deleteByEventStatus(status)
    }
}