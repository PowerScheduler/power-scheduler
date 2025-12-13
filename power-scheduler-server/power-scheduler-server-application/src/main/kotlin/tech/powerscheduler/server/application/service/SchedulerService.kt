package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.server.application.schedule.system.ServerAddressHolder
import tech.powerscheduler.server.domain.scheduler.Scheduler
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/12/13
 */
@Service
class SchedulerService(
    private val serverAddressHolder: ServerAddressHolder,
    private val schedulerRepository: SchedulerRepository,
    private val transactionTemplate: TransactionTemplate,
) {

    fun registerCurrentScheduler() {
        val address = serverAddressHolder.address
        val existScheduler = schedulerRepository.findByAddress(address)
        transactionTemplate.execute {
            if (existScheduler != null) {
                schedulerRepository.lockById(existScheduler.id!!)
            }
            val schedulerToSave = Scheduler().apply {
                this.id = existScheduler?.id
                this.online = true
                this.address = address
                this.lastHeartbeatAt = LocalDateTime.now()
            }
            schedulerRepository.save(schedulerToSave)
        }
    }

    fun removeCurrentScheduler() {
        val address = serverAddressHolder.address
        val existScheduler = schedulerRepository.findByAddress(address)
        if (existScheduler != null) {
            existScheduler.online = false
            schedulerRepository.save(existScheduler)
        }
    }
}