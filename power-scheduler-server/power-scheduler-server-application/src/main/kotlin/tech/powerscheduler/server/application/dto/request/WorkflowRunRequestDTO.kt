package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/5
 */
class WorkflowRunRequestDTO {
    /**
     * 任务id
     */
    @NotNull
    var workflowId: Long? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = LocalDateTime.now()
}