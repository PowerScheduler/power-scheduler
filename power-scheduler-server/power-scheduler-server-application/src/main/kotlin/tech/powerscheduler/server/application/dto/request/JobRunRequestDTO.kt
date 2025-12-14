package tech.powerscheduler.server.application.dto.request

import java.time.LocalDateTime

/**
 * 任务执行一次请求参数
 *
 * @author grayrat
 * @since 2025/4/16
 */
class JobRunRequestDTO {
    /**
     * Worker地址（ip:host）
     */
    var workerAddress: String? = null

    /**
     * 任务参数
     */
    var executeParams: String? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = LocalDateTime.now()
}