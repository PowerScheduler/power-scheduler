package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/5/30
 */
class DashboardStatisticsInfoQueryRequestDTO {
    /**
     * 应用编码
     */
    var appCode: String? = null

    /**
     * 调度时间范围
     */
    @NotNull
    var scheduleAtRange: Array<LocalDateTime>? = null
}