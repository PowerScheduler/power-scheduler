package tech.powerscheduler.server.infrastructure.persistence.repository.impl

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import tech.powerscheduler.server.infrastructure.persistence.model.SchedulerEntity

/**
 * @author grayrat
 * @since 2025/7/11
 */
interface SchedulerJpaRepository :
    JpaRepository<SchedulerEntity, Long>, JpaSpecificationExecutor<SchedulerEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM SchedulerEntity t WHERE t.id = :id")
    fun findByIdForUpdate(id: Long): SchedulerEntity?

    fun findByAddress(address: String): SchedulerEntity?

}