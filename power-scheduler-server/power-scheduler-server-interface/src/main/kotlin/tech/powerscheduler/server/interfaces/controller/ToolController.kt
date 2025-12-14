package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.powerscheduler.server.application.dto.request.CronParseRequestDTO
import tech.powerscheduler.server.application.service.ToolService

/**
 * @author grayrat
 * @since 2025/5/30
 */
@Tag(name = "ToolApi")
@Validated
@RestController
@RequestMapping(TOOL_API)
class ToolController(
    private val toolService: ToolService,
) : BaseController() {

    @Operation(summary = "解析CRON表达式")
    @GetMapping("/cron/parse")
    fun parseCron(
        @Validated param: CronParseRequestDTO
    ) = wrapperResponse {
        toolService.parseCron(param)
    }
}