package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
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
@RequestMapping("/api/workflows")
class WorkflowController(
    private val workflowService: WorkflowService,
) : BaseController() {

    @Operation(summary = "查询工作流列表")
    @GetMapping("/list")
    fun listWorkflow(@Validated @NotNull param: WorkflowQueryRequestDTO?) = wrapperResponse {
        return@wrapperResponse workflowService.list(param!!)
    }

    @Operation(summary = "查询工作流详情")
    @GetMapping("/detail")
    fun getWorkflow(@Validated @NotNull workflowId: Long?) = wrapperResponse {
        return@wrapperResponse workflowService.get(workflowId!!)
    }

    @Operation(summary = "新增工作流")
    @PostMapping("/add")
    fun addWorkflow(@RequestBody @Validated @NotNull param: WorkflowAddRequestDTO?) = wrapperResponse {
        return@wrapperResponse workflowService.add(param!!)
    }

    @Operation(summary = "编辑工作流")
    @PostMapping("/edit")
    fun editWorkflow(@RequestBody @Validated @NotNull param: WorkflowEditRequestDTO?) = wrapperResponse {
        return@wrapperResponse workflowService.edit(param!!)
    }

    @Operation(summary = "修改工作流启用状态")
    @PostMapping("/switch")
    fun switchWorkflowEnable(@Validated @RequestBody param: WorkflowSwitchRequestDTO) = wrapperResponse {
        return@wrapperResponse workflowService.switch(param)
    }

    @Operation(summary = "删除工作流")
    @PostMapping("/delete")
    fun deleteWorkflow(@Validated @NotNull workflowId: Long?) = wrapperResponse {
        return@wrapperResponse workflowService.delete(workflowId!!)
    }

    @Operation(summary = "运行工作流")
    @PostMapping("/run")
    fun runWorkflow(@NotNull @Validated @RequestBody param: WorkflowRunRequestDTO?) = wrapperResponse {
        return@wrapperResponse workflowService.run(param!!)
    }
}