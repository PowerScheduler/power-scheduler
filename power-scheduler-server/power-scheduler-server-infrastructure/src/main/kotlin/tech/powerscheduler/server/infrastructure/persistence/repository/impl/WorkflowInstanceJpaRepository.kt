package tech.powerscheduler.server.infrastructure.persistence.repository.impl

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.workflow.WorkflowId
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowInstanceEntity
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowInstanceJpaRepository
    : JpaRepository<WorkflowInstanceEntity, Long>, JpaSpecificationExecutor<WorkflowInstanceEntity> {

    @Query(
        """
        SELECT wi.workflowId, COUNT(wi) 
        FROM WorkflowInstanceEntity wi 
        WHERE true 
          AND wi.workflowId IN :workflowIds 
          AND wi.status IN :statuses 
        GROUP BY wi.workflowId
    """
    )
    fun countGroupByWorkflowIdAndStatus(
        workflowIds: List<Long>,
        statuses: Set<WorkflowStatusEnum>
    ): List<Array<Any>>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WorkflowInstanceEntity t WHERE t.id = :id")
    fun findByIdForUpdate(id: Long): WorkflowInstanceEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WorkflowInstanceEntity t WHERE t.code = :code")
    fun findByCodeForUpdate(code: String): WorkflowInstanceEntity?

    fun findAllByWorkflowIdAndStatusIn(
        workflowId: WorkflowId,
        statuses: Set<WorkflowStatusEnum>,
        pageable: Pageable
    ): Page<WorkflowInstanceEntity>

    fun findAllByWorkflowIdAndStatusInAndEndAtBefore(
        workflowId: WorkflowId,
        statuses: Set<WorkflowStatusEnum>,
        endAt: LocalDateTime,
        pageable: PageRequest
    ): Page<WorkflowInstanceEntity>

}