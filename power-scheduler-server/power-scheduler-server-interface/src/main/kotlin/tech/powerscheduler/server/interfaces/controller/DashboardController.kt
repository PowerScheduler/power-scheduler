package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.powerscheduler.server.application.dto.request.DashboardBasicInfoQueryRequestDTO
import tech.powerscheduler.server.application.dto.request.DashboardStatisticsInfoQueryRequestDTO
import tech.powerscheduler.server.application.service.DashboardService

/**
 * @author grayrat
 * @since 2025/5/20
 */
@Tag(name = "DashboardApi")
@Validated
@RestController
@RequestMapping(DASHBOARD_API)
internal class DashboardController(
    private val dashboardService: DashboardService,
) : BaseController() {

    @Operation(summary = "查询基本信息")
    @GetMapping("/{namespaceCode}/basicInfo")
    fun queryBasicInfo(
        @PathVariable @NotBlank namespaceCode: String,
        @Validated param: DashboardBasicInfoQueryRequestDTO
    ) = wrapperResponse {
        dashboardService.queryBasicInfo(
            namespaceCode = namespaceCode,
            param = param,
        )
    }

    @Operation(summary = "查询统计信息")
    @GetMapping("/{namespaceCode}/statisticsInfo")
    fun queryStatisticsInfo(
        @PathVariable @NotBlank namespaceCode: String,
        @Validated param: DashboardStatisticsInfoQueryRequestDTO
    ) = wrapperResponse {
        dashboardService.queryStatisticsInfo(
            namespaceCode = namespaceCode,
            param = param
        )
    }
}