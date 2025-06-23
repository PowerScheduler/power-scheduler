package tech.powerscheduler.common.dto.request

import tech.powerscheduler.common.enums.*
import java.time.LocalDateTime

/**
 * 任务下发请求参数
 *
 * @author grayrat
 * @since 2025/5/26
 */
class JobDispatchRequestDTO {
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
     * 子任务的父任务id
     */
    var parentTaskId: Long? = null

    /**
     * 应用编码
     */
    var appCode: String? = null

    /**
     * 任务类型
     */
    var jobType: JobTypeEnum? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 任务状态
     */
    var jobStatus: JobStatusEnum? = null

    /**
     * 执行参数
     */
    var executeParams: String? = null

    /**
     * 调度时间
     */
    var scheduleAt: LocalDateTime? = null

    /**
     * 执行模式
     */
    var executeMode: ExecuteModeEnum? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = null

    /**
     * 脚本类型
     */
    var scriptType: ScriptTypeEnum? = null

    /**
     * 脚本编码
     */
    var scriptCode: String? = null

    /**
     * 重试次数
     */
    var attemptCnt: Int? = null

    /**
     * 任务优先级
     */
    var priority: Int = 0

    /**
     * 子任务内容(用于存储 Map 和 MapReduce模式下用户自定义的任务参数)
     */
    var taskBody: String? = null

    /**
     * 子任务类型
     */
    var taskType: TaskTypeEnum? = null
}