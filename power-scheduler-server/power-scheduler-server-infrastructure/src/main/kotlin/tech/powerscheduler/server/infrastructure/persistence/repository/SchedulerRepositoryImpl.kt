package tech.powerscheduler.server.infrastructure.persistence.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import tech.powerscheduler.server.domain.scheduler.Scheduler
import tech.powerscheduler.server.domain.scheduler.SchedulerId
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import tech.powerscheduler.server.infrastructure.persistence.model.SchedulerEntity
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.SchedulerJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toEntity
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/11
 */
@Component
class SchedulerRepositoryImpl(
    private val schedulerJpaRepository: SchedulerJpaRepository,
) : SchedulerRepository {

    override fun findAll(): List<Scheduler> {
        val entities = schedulerJpaRepository.findAll()
        return entities.map { it.toDomainModel() }
    }

    override fun lockById(id: SchedulerId): Scheduler? {
        val entity = schedulerJpaRepository.findByIdForUpdate(id.value)
        return entity?.toDomainModel()
    }

    override fun findByAddress(address: String): Scheduler? {
        val entity = schedulerJpaRepository.findByAddress(address)
        return entity?.toDomainModel()
    }

    override fun findAllExpired(): List<Scheduler> {
        val specification = Specification<SchedulerEntity> { root, _, criteriaBuilder ->
            val expiredAt = LocalDateTime.now().minusSeconds(Scheduler.EXPIRE_TIME)
            val onlineEqual = criteriaBuilder.equal(
                root.get<String>(SchedulerEntity::online.name), false
            )
            val lastHeartbeatAtLessThan = criteriaBuilder.lessThan(
                root.get(SchedulerEntity::lastHeartbeatAt.name),
                expiredAt
            )
            criteriaBuilder.and(onlineEqual, lastHeartbeatAtLessThan)
        }
        val entities = schedulerJpaRepository.findAll(specification)
        return entities.map { it.toDomainModel() }
    }

    override fun save(scheduler: Scheduler): SchedulerId {
        val entity = scheduler.toEntity()
        schedulerJpaRepository.save(entity)
        return SchedulerId(entity.id!!)
    }

    override fun remove(id: SchedulerId) {
        schedulerJpaRepository.deleteById(id.value)
    }

}