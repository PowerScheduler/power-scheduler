package tech.powerscheduler.server.domain.domainevent

/**
 * @author grayrat
 * @since 2025/6/8
 */
class DomainEvent {
    /**
     * 主键id
     */
    var id: DomainEventId? = null

    /**
     * 聚合根id
     */
    var aggregateId: String? = null

    /**
     * 聚合根类型
     */
    var aggregateType: AggregateTypeEnum? = null

    /**
     * 事件类型
     */
    var eventType: DomainEventTypeEnum? = null

    /**
     * 事件状态
     */
    var eventStatus: DomainEventStatusEnum? = null

    /**
     * 事件内容
     */
    var body: String? = null

    /**
     * 重试次数
     */
    var retryCnt: Int? = null

    val canRetry
        get() = this.retryCnt!! < 3

    fun resetStatusForRetry() {
        this.eventStatus = DomainEventStatusEnum.PENDING
        this.retryCnt = this.retryCnt!! + 1
    }

    companion object {
        fun create(
            aggregateId: String,
            aggregateType: AggregateTypeEnum,
            eventType: DomainEventTypeEnum,
            body: String?,
        ): DomainEvent {
            return DomainEvent().apply {
                this.eventStatus = DomainEventStatusEnum.PENDING
                this.aggregateId = aggregateId
                this.aggregateType = aggregateType
                this.eventType = eventType
                this.body = body
                this.retryCnt = 0
            }
        }
    }
}