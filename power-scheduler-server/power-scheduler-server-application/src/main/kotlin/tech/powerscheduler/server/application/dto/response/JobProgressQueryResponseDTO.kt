package tech.powerscheduler.server.application.dto.response

import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/14
 */
class JobProgressQueryResponseDTO {
    /**
     * 子任务id
     */
    var taskId: Long? = null

    /**
     * 子任务名称
     */
    var taskName: String? = null

    /**
     * 任务实例id
     */
    var jobInstanceId: Long? = null

    /**
     * 子任务状态
     */
    var taskStatus: EnumDTO? = null

    /**
     * 执行器地址
     */
    var workerAddress: String? = null

    /**
     * 开始时间
     */
    var startAt: LocalDateTime? = null

    /**
     * 结束时间
     */
    var endAt: LocalDateTime? = null
}