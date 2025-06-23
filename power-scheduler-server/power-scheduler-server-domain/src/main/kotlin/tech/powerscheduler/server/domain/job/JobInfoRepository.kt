package tech.powerscheduler.server.domain.job

import tech.powerscheduler.server.domain.common.Enabled
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import java.time.LocalDateTime

/**
 * 任务信息持久化仓库
 *
 * @author grayrat
 * @since 2025/4/16
 */
interface JobInfoRepository {

    fun countGroupedByEnabledWithAppCode(namespaceCode: String, appCode: String): Map<Enabled, Long>

    fun lockById(id: JobId): JobInfo?

    fun pageQuery(query: JobInfoQuery): Page<JobInfo>

    fun findById(id: JobId): JobInfo?

    fun findAllByIds(ids: Iterable<JobId>): List<JobInfo>

    fun clearSchedulerByAddress(schedulerAddress: String)

    fun save(jobInfo: JobInfo): JobId

    fun deleteById(id: JobId)

    fun listAllIds(pageQuery: PageQuery): Page<JobId>

    fun listAssignableIds(pageQuery: PageQuery): Page<JobId>

    fun findSchedulableByIds(ids: Iterable<JobId>, baseTime: LocalDateTime): List<JobInfo>

    fun listIdsByEnabledAndSchedulerAddress(
        enabled: Boolean?,
        schedulerAddress: String,
        pageQuery: PageQuery
    ): Page<JobId>
}