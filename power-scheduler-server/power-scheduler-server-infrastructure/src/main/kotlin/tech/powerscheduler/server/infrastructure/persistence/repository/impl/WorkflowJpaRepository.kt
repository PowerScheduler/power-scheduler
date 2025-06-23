package tech.powerscheduler.server.infrastructure.persistence.repository.impl

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.*
import org.springframework.transaction.annotation.Transactional
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowEntity

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowJpaRepository
    : JpaRepository<WorkflowEntity, Long>, JpaSpecificationExecutor<WorkflowEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WorkflowEntity t WHERE t.id = :id")
    fun findByIdForUpdate(id: Long): WorkflowEntity?

    @Query(
        """
        SELECT w.id 
        FROM WorkflowEntity w 
        WHERE true
            AND (:enabled IS NULL OR w.enabled = :enabled) 
            AND w.schedulerAddress = :schedulerAddress
    """
    )
    fun listIdsByEnabledAndSchedulerAddress(
        enabled: Boolean?,
        schedulerAddress: String,
        pageable: Pageable
    ): Page<Long>

    @Transactional
    @Modifying
    @Query(
        """
        UPDATE WorkflowEntity t
        SET t.schedulerAddress = null 
        WHERE true
            AND t.schedulerAddress = :address
    """
    )
    fun clearSchedulerByAddress(address: String)
}