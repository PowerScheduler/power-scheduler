package tech.powerscheduler.server.application.dto.request

import jakarta.validation.constraints.NotNull

/**
 * @author grayrat
 * @since 2025/7/3
 */
class WorkflowSwitchRequestDTO {
    /**
     * 开关状态
     */
    @NotNull
    var enabled: Boolean? = null
}