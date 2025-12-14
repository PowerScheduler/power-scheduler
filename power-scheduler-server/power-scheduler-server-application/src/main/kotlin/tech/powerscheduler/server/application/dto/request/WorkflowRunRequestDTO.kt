package tech.powerscheduler.server.application.dto.request

import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/5
 */
class WorkflowRunRequestDTO {
    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = LocalDateTime.now()
}