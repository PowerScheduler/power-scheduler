package tech.powerscheduler.worker.task

import tech.powerscheduler.common.enums.TaskTypeEnum
import java.time.LocalDateTime

/**
 * 任务上下文
 *
 * @author grayrat
 * @since 2025/4/26
 */
open class TaskContext {
    /**
     * 任务实例id
     */
    var jobInstanceId: Long? = null

    /**
     * 子任务id
     */
    var taskId: Long? = null

    /**
     * 子任务名称
     */
    var taskName: String? = null

    /**
     * 任务执行参数
     */
    var executeParams: String? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = null

    /**
     * 子任务类型
     */
    var taskType: TaskTypeEnum? = null
}
