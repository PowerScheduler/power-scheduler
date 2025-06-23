package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/5/30
 */
class DashboardStatisticsInfoQueryRequestDTO {
    /**
     * 命名空间编码
     */
    @NotBlank
    var namespaceCode: String? = null

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