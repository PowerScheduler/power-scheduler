package tech.powerscheduler.server.domain.domainevent

import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery

/**
 * @author grayrat
 * @since 2025/6/8
 */
interface DomainEventRepository {

    fun findPendingList(
        eventType: DomainEventTypeEnum,
        pageQuery: PageQuery,
    ): Page<DomainEvent>

    fun save(domainEvent: DomainEvent): DomainEventId

    fun deleteByIds(ids: Iterable<DomainEventId>)

    fun deleteByEventStatus(status: DomainEventStatusEnum)

}