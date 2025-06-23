package tech.powerscheduler.worker.persistence

import tech.powerscheduler.common.enums.JobStatusEnum
import java.time.LocalDateTime

/**
 * 任务进度实体模型
 *
 * @author grayrat
 * @since 2025/5/25
 */
class TaskProgressEntity {
    var id: Long? = null
    var jobInstanceId: Long? = null
    var taskId: Long? = null
    var status: JobStatusEnum? = null
    var startAt: LocalDateTime? = null
    var endAt: LocalDateTime? = null
    var result: String? = null

    /**
     * 子任务列表
     */
    var subTaskListBody: String? = null

    /**
     * 子任务名称
     */
    var subTaskName: String? = null
}