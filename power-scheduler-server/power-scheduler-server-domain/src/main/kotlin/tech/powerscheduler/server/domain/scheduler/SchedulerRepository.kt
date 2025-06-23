package tech.powerscheduler.server.domain.scheduler

/**
 * @author grayrat
 * @since 2025/7/11
 */
interface SchedulerRepository {

    fun lockById(id: SchedulerId): Scheduler?

    fun findAll(): List<Scheduler>

    fun findByAddress(address: String): Scheduler?

    fun findAllExpired(): List<Scheduler>

    fun save(scheduler: Scheduler): SchedulerId

    fun remove(id: SchedulerId)

}