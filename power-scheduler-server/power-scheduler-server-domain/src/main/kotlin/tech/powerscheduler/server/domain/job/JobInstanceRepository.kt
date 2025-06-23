package tech.powerscheduler.server.domain.job

import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.common.PageQuery
import java.time.LocalDateTime

/**
 * 任务实例持计划仓库
 *
 * @author grayrat
 * @since 2025/4/16
 */
interface JobInstanceRepository {

    fun countGroupedByJobStatusWithAppCode(
        namespaceCode: String,
        appCode: String,
        scheduleAtRange: Array<LocalDateTime>
    ): Map<JobStatusEnum, Long>

    fun lockById(jobInstanceId: JobInstanceId): JobInstance?

    fun findById(jobInstanceId: JobInstanceId): JobInstance?

    fun pageQuery(query: JobInstanceQuery): Page<JobInstance>

    fun save(jobInstance: JobInstance): JobInstanceId

    fun saveAll(jobInstanceList: List<JobInstance>): List<JobInstanceId>

    /**
     * 根据 任务id, 任务状态 查询符合条件的任务实例的数量
     * 用途: 1. 检查任务并发数量, 决定任务是否调度任务 2. 检查任务实例是否超过保留数量, 决定是否清理任务实例
     *
     * @param jobIds
     * @param jobStatuses
     * @return
     */
    fun countByJobIdAndJobStatus(
        jobIds: Iterable<JobId>,
        jobStatuses: Iterable<JobStatusEnum>
    ): Map<JobId, Long>

    /**
     * 根据 任务id, 任务状态 查询符合条件的任务实例id列表
     * 用途: 清理超出保留数量的任务实例
     *
     * @param jobId
     * @param jobStatuses
     * @param endAt
     * @param pageQuery
     * @return
     */
    fun listIdByJobIdAndJobStatus(
        jobId: JobId,
        jobStatuses: Iterable<JobStatusEnum>,
        pageQuery: PageQuery
    ): Page<JobInstanceId>

    /**
     * 根据 任务id, 任务状态 和 完成时间 查询符合条件的任务实例id列表
     * 用途: 清理过期的任务实例
     *
     * @param jobId
     * @param jobStatuses
     * @param endAt
     * @param pageQuery
     * @return
     */
    fun listIdByJobIdAndJobStatusAndEndAtBefore(
        jobId: JobId,
        jobStatuses: Set<JobStatusEnum>,
        endAt: LocalDateTime,
        pageQuery: PageQuery
    ): Page<JobInstanceId>

    /**
     * 根据 任务来源, 任务来源对象id 分页查询可分发给worker的任务实例
     * 用途: 1. 常规任务实例分发 2. 工作流节点任务实例分发
     *
     * @param sourceIds
     * @param pageQuery
     * @return
     */
    fun listDispatchable(
        sourceIds: Iterable<SourceId>,
        sourceType: JobSourceTypeEnum,
        pageQuery: PageQuery
    ): Page<JobInstanceId>

    fun deleteByIds(ids: Iterable<JobInstanceId>)
}