package tech.powerscheduler.server.infrastructure.utils

import tech.powerscheduler.server.domain.scheduler.Scheduler
import tech.powerscheduler.server.domain.scheduler.SchedulerId
import tech.powerscheduler.server.infrastructure.persistence.model.SchedulerEntity

/**
 * @author grayrat
 * @since 2025/7/11
 */
fun SchedulerEntity.toDomainModel(): Scheduler {
    return Scheduler().also {
        it.id = SchedulerId(this.id!!)
        it.online = this.online
        it.address = this.address
        it.lastHeartbeatAt = this.lastHeartbeatAt
    }
}

fun Scheduler.toEntity(): SchedulerEntity {
    return SchedulerEntity().also {
        it.id = this.id?.value
        it.online = this.online
        it.address = this.address
        it.lastHeartbeatAt = this.lastHeartbeatAt
    }
}