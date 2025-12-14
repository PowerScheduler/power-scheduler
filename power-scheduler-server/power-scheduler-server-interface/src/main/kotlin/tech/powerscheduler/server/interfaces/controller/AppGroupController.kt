package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import tech.powerscheduler.server.application.context.UserContext
import tech.powerscheduler.server.application.dto.request.AppGroupAddRequestDTO
import tech.powerscheduler.server.application.dto.request.AppGroupEditRequestDTO
import tech.powerscheduler.server.application.dto.request.AppGroupQueryRequestDTO
import tech.powerscheduler.server.application.service.AppGroupService

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Tag(name = "AppGroupApi")
@Validated
@RestController
@RequestMapping(APP_GROUP_API)
internal class AppGroupController(
    private var appGroupService: AppGroupService,
) : BaseController() {

    @Operation(summary = "查询应用分组")
    @GetMapping("/")
    fun listAppGroup(
        @Validated param: AppGroupQueryRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse appGroupService.list(param)
    }

    @Operation(summary = "新增应用分组")
    @PostMapping("/")
    fun addAppGroup(
        @RequestBody @Validated param: AppGroupAddRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse appGroupService.add(param, UserContext())
    }

    @Operation(summary = "编辑应用分组")
    @PutMapping("/{appGroupId}")
    fun editAppGroup(
        @PathVariable appGroupId: Long,
        @RequestBody @Validated param: AppGroupEditRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse appGroupService.edit(appGroupId, param, UserContext())
    }
}