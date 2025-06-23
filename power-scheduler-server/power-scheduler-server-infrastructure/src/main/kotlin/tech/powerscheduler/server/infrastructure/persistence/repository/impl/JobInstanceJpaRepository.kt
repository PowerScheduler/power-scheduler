package tech.powerscheduler.server.infrastructure.persistence.repository.impl

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.server.infrastructure.persistence.model.JobInstanceEntity
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Repository
interface JobInstanceJpaRepository :
    JpaRepository<JobInstanceEntity, Long>, JpaSpecificationExecutor<JobInstanceEntity> {

    @Query(
        """
        SELECT j.jobStatus, COUNT(j)
        FROM JobInstanceEntity j 
        JOIN j.appGroupEntity a
        JOIN a.namespaceEntity n
        WHERE TRUE
          AND (n.code = :namespaceCode)
          AND (a.code = :appCode or :appCode = '')
          AND j.scheduleAt >= :scheduleAtRangeStart
          AND j.scheduleAt <= :scheduleAtRangeEnd
        GROUP BY j.jobStatus
    """
    )
    fun countGroupedByJobStatusWithAppCode(
        namespaceCode: String,
        appCode: String,
        scheduleAtRangeStart: LocalDateTime,
        scheduleAtRangeEnd: LocalDateTime,
    ): List<Array<Any>>

    @Query(
        """
        SELECT j.sourceId, COUNT(j) 
        FROM JobInstanceEntity j 
        WHERE true 
          AND j.sourceType = :sourceType
          AND j.sourceId IN :sourceIds 
          AND j.jobStatus IN :jobStatuses 
        GROUP BY j.sourceId
    """
    )
    fun countGroupBySourceIdAndJobStatus(
        sourceIds: Iterable<Long>,
        sourceType: JobSourceTypeEnum,
        jobStatuses: Iterable<JobStatusEnum>
    ): List<Array<Any>>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM JobInstanceEntity t WHERE t.id = :id")
    fun lockById(id: Long): JobInstanceEntity?

    @Query(
        """
        SELECT 
            jobInstance.id
        FROM JobInstanceEntity AS jobInstance
        WHERE true 
          AND jobInstance.sourceId = :sourceId 
          AND jobInstance.jobStatus IN :jobStatuses
    """
    )
    fun listIdBySourceIdAndJobStatus(
        sourceId: Long,
        jobStatuses: Iterable<JobStatusEnum>,
        pageable: Pageable
    ): Page<Long>

    @Query(
        """
        SELECT 
            jobInstance.id
        FROM JobInstanceEntity AS jobInstance
        WHERE true 
          AND jobInstance.sourceId = :sourceId 
          AND jobInstance.sourceType = :sourceType
          AND jobInstance.jobStatus IN :jobStatuses
          AND jobInstance.endAt < :endAt
    """
    )
    fun listIdBySourceIdAndJobStatusAndEndAtBefore(
        sourceId: Long,
        sourceType: JobSourceTypeEnum,
        jobStatuses: Set<JobStatusEnum>,
        endAt: LocalDateTime,
        pageable: Pageable
    ): Page<Long>

    @Query(
        """
        SELECT 
            jobInstance.id
        FROM JobInstanceEntity AS jobInstance
        WHERE true 
          AND jobInstance.sourceId IN :sourceIds
          AND jobInstance.sourceType = :sourceType
          AND jobInstance.jobStatus IN :jobStatuses
    """
    )
    fun listDispatchable(
        sourceIds: Iterable<Long>,
        sourceType: JobSourceTypeEnum,
        jobStatuses: Iterable<JobStatusEnum>,
        pageRequest: Pageable,
    ): Page<Long>

}