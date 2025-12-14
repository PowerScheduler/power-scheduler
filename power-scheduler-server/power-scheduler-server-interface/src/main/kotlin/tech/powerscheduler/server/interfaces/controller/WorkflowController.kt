package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import tech.powerscheduler.server.application.dto.request.*
import tech.powerscheduler.server.application.service.WorkflowService

/**
 * @author grayrat
 * @since 2025/6/23
 */
@Tag(name = "WorkflowApi")
@Validated
@RestController
@RequestMapping(WORKFLOW_API)
class WorkflowController(
    private val workflowService: WorkflowService,
) : BaseController() {

    @Operation(summary = "查询工作流列表")
    @GetMapping("/")
    fun listWorkflow(
        @Validated param: WorkflowQueryRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowService.list(param)
    }

    @Operation(summary = "查询工作流详情")
    @GetMapping("/{workflowId}")
    fun getWorkflow(
        @PathVariable workflowId: Long
    ) = wrapperResponse {
        return@wrapperResponse workflowService.get(workflowId)
    }

    @Operation(summary = "新增工作流")
    @PostMapping("/")
    fun addWorkflow(
        @RequestBody @Validated param: WorkflowAddRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowService.add(param)
    }

    @Operation(summary = "编辑工作流")
    @PutMapping("/{workflowId}")
    fun editWorkflow(
        @PathVariable workflowId: Long,
        @RequestBody @Validated param: WorkflowEditRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowService.edit(
            workflowId = workflowId,
            param = param
        )
    }

    @Operation(summary = "修改工作流启用状态")
    @PatchMapping("/{workflowId}/status")
    fun switchWorkflowStatus(
        @PathVariable workflowId: Long,
        @Validated @RequestBody param: WorkflowSwitchRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowService.switch(
            workflowId = workflowId,
            param = param
        )
    }

    @Operation(summary = "删除工作流")
    @DeleteMapping("/{workflowId}")
    fun deleteWorkflow(
        @PathVariable workflowId: Long
    ) = wrapperResponse {
        return@wrapperResponse workflowService.delete(workflowId)
    }

    @Operation(summary = "运行工作流")
    @PostMapping("/{workflowId}/run")
    fun runWorkflow(
        @PathVariable workflowId: Long,
        @Validated @RequestBody param: WorkflowRunRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowService.run(
            workflowId = workflowId,
            param = param
        )
    }
}