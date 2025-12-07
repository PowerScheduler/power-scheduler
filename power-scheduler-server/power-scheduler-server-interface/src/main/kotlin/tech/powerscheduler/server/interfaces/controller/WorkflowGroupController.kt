package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.powerscheduler.server.application.context.UserContext
import tech.powerscheduler.server.application.dto.request.WorkflowGroupAddRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowGroupEditRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowGroupQueryRequestDTO
import tech.powerscheduler.server.application.service.WorkflowGroupService

/**
 * @author grayrat
 * @since 2025/12/7
 */
@Tag(name = "WorkflowGroupApi")
@Validated
@RestController
@RequestMapping("/api/workflowGroups")
class WorkflowGroupController(
    private val workflowGroupService: WorkflowGroupService,
) : BaseController() {

    @Operation(summary = "查工作流分组")
    @PostMapping("/list")
    fun listWorkflowGroup(@RequestBody @Validated param: WorkflowGroupQueryRequestDTO) = wrapperResponse {
        return@wrapperResponse workflowGroupService.list(param)
    }

    @Operation(summary = "新建工作流分组")
    @PostMapping("/add")
    fun addWorkflowGroup(@RequestBody @Validated param: WorkflowGroupAddRequestDTO) = wrapperResponse {
        return@wrapperResponse workflowGroupService.add(param, UserContext())
    }

    @Operation(summary = "编辑工作流分组")
    @PostMapping("/edit")
    fun editWorkflowGroup(@RequestBody @Validated param: WorkflowGroupEditRequestDTO) = wrapperResponse {
        return@wrapperResponse workflowGroupService.edit(param, UserContext())
    }

}