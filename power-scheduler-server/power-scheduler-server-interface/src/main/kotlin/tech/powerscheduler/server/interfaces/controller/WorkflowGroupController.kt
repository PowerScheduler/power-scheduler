package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
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
@RequestMapping(WORKFLOW_GROUP_API)
class WorkflowGroupController(
    private val workflowGroupService: WorkflowGroupService,
) : BaseController() {

    @Operation(summary = "查工作流分组")
    @GetMapping("/")
    fun listWorkflowGroup(
        @Validated param: WorkflowGroupQueryRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowGroupService.list(param)
    }

    @Operation(summary = "新建工作流分组")
    @PostMapping("/")
    fun addWorkflowGroup(
        @RequestBody @Validated param: WorkflowGroupAddRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowGroupService.add(
            param = param,
            userContext = UserContext()
        )
    }

    @Operation(summary = "编辑工作流分组")
    @PutMapping("/{workflowGroupId}")
    fun editWorkflowGroup(
        @PathVariable workflowGroupId: Long,
        @RequestBody @Validated param: WorkflowGroupEditRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowGroupService.edit(
            workflowGroupId = workflowGroupId,
            param = param,
            userContext = UserContext(),
        )
    }

}