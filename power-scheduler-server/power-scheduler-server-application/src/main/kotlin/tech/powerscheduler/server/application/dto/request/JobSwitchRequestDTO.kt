package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotNull

/**
 * 任务开关切换请求参数
 *
 * @author grayrat
 * @since 2025/4/16
 */
class JobSwitchRequestDTO {
    /**
     * 开关状态
     */
    @NotNull
    var enabled: Boolean? = null
}