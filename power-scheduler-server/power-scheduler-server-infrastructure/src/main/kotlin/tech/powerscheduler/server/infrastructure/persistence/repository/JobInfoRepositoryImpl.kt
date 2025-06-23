package tech.powerscheduler.server.infrastructure.persistence.repository

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import tech.powerscheduler.server.domain.common.Enabled
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobId
import tech.powerscheduler.server.domain.job.JobInfo
import tech.powerscheduler.server.domain.job.JobInfoQuery
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.infrastructure.persistence.model.AppGroupEntity
import tech.powerscheduler.server.infrastructure.persistence.model.JobInfoEntity
import tech.powerscheduler.server.infrastructure.persistence.model.NamespaceEntity
import tech.powerscheduler.server.infrastructure.persistence.repository.impl.JobInfoJpaRepository
import tech.powerscheduler.server.infrastructure.utils.toDomainModel
import tech.powerscheduler.server.infrastructure.utils.toDomainPage
import tech.powerscheduler.server.infrastructure.utils.toEntity
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Repository
class JobInfoRepositoryImpl(
    private val jobInfoJpaRepository: JobInfoJpaRepository
) : JobInfoRepository {

    override fun countGroupedByEnabledWithAppCode(
        namespaceCode: String,
        appCode: String
    ): Map<Enabled, Long> {
        val countResult = jobInfoJpaRepository.countGroupedByEnabledWithAppCode(
            namespaceCode = namespaceCode,
            appCode = appCode,
        )
        return countResult.associate { it[0] as Boolean to it[1] as Long }
    }

    override fun lockById(id: JobId): JobInfo? {
        val entity = jobInfoJpaRepository.findByIdForUpdate(id.value)
        return entity?.toDomainModel()
    }

    override fun pageQuery(query: JobInfoQuery): Page<JobInfo> {
        val pageable = PageRequest.of(
            query.pageNo - 1, query.pageSize, Sort.by(JobInfoEntity::id.name).descending()
        )
        val specification = Specification<JobInfoEntity> { root, _, criteriaBuilder ->
            val appGroupJoin = root.join<JobInfoEntity, AppGroupEntity>(
                JobInfoEntity::appGroupEntity.name, JoinType.INNER
            )
            val namespaceJoin = appGroupJoin.join<AppGroupEntity, NamespaceEntity>(
                AppGroupEntity::namespaceEntity.name, JoinType.INNER
            )
            val namespaceCodeEquals = criteriaBuilder.equal(
                namespaceJoin.get<String>(NamespaceEntity::code.name), query.namespaceCode
            )
            val appCodeEqual = query.appCode.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.equal(appGroupJoin.get<String>(AppGroupEntity::code.name), it)
            }
            val jobNameLike = query.jobName.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.like(root.get(JobInfoEntity::jobName.name), "%$it%")
            }
            val processorLike = query.processor.takeUnless { it.isNullOrBlank() }?.let {
                criteriaBuilder.like(root.get(JobInfoEntity::processor.name), "%$it%")
            }
            val predicates = listOfNotNull(namespaceCodeEquals, appCodeEqual, jobNameLike, processorLike)
            criteriaBuilder.and(*predicates.toTypedArray())
        }
        val page = jobInfoJpaRepository.findAll(specification, pageable)
        return page.map { it.toDomainModel() }.toDomainPage()
    }

    override fun findById(id: JobId): JobInfo? {
        val jobInfoEntity = jobInfoJpaRepository.findByIdOrNull(id.value)
        return jobInfoEntity?.toDomainModel()
    }

    override fun findAllByIds(ids: Iterable<JobId>): List<JobInfo> {
        val list = jobInfoJpaRepository.findAllById(ids.map { it.value })
        return list.map { it.toDomainModel() }
    }

    override fun clearSchedulerByAddress(schedulerAddress: String) {
        jobInfoJpaRepository.clearSchedulerByAddress(schedulerAddress)
    }

    override fun save(jobInfo: JobInfo): JobId {
        val jobInfoEntity = jobInfo.toEntity()
        jobInfoJpaRepository.save(jobInfoEntity)
        return JobId(jobInfoEntity.id!!)
    }

    override fun deleteById(id: JobId) {
        jobInfoJpaRepository.deleteById(id.value)
    }

    override fun listAllIds(pageQuery: PageQuery): Page<JobId> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1, pageQuery.pageSize, Sort.by(JobInfoEntity::id.name).descending()
        )
        val page = jobInfoJpaRepository.findAllIds(pageable)
        return page.map { JobId(it) }.toDomainPage()
    }

    override fun listAssignableIds(pageQuery: PageQuery): Page<JobId> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1,
            pageQuery.pageSize,
            Sort.by(JobInfoEntity::id.name).descending()
        )
        val specification = Specification<JobInfoEntity> { root, _, criteriaBuilder ->
            val notAssigned = criteriaBuilder.isNull(root.get<Boolean>(JobInfoEntity::schedulerAddress.name))
            criteriaBuilder.and(notAssigned)
        }
        val page = jobInfoJpaRepository.findAll(specification, pageable)
        return page.map { JobId(it.id!!) }.toDomainPage()
    }

    override fun findSchedulableByIds(
        ids: Iterable<JobId>,
        baseTime: LocalDateTime,
    ): List<JobInfo> {
        val nextScheduleAt = baseTime.plusSeconds(5)
        val specification = Specification<JobInfoEntity> { root, _, cb ->
            val idIn = root.get<Long>(JobInfoEntity::id.name).`in`(ids.map { it.value })
            val isEnabled = cb.equal(root.get<Boolean>(JobInfoEntity::enabled.name), true)
            val nextScheduleAtGreatEquals =
                cb.lessThanOrEqualTo(root.get(JobInfoEntity::nextScheduleAt.name), nextScheduleAt)
            cb.and(
                isEnabled,
                idIn,
                nextScheduleAtGreatEquals,
            )
        }
        val list = jobInfoJpaRepository.findAll(specification)
        return list.map { it.toDomainModel() }
    }

    override fun listIdsByEnabledAndSchedulerAddress(
        enabled: Boolean?,
        schedulerAddress: String,
        pageQuery: PageQuery
    ): Page<JobId> {
        val pageable = PageRequest.of(
            pageQuery.pageNo - 1, pageQuery.pageSize, Sort.by(JobInfoEntity::id.name).descending()
        )
        val page = jobInfoJpaRepository.findIdByEnabledAndSchedulerAddress(
            enabled = enabled,
            schedulerAddress = schedulerAddress,
            pageable = pageable,
        )
        return page.map { JobId(it) }.toDomainPage()
    }

}