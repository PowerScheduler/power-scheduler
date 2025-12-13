package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.powerscheduler.server.application.dto.request.WorkflowInstanceQueryRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowNodeInstanceProgressQueryRequestDTO
import tech.powerscheduler.server.application.service.WorkflowInstanceService

/**
 * @author grayrat
 * @since 2025/7/9
 */
@Tag(name = "WorkflowInstanceApi")
@Validated
@RestController
@RequestMapping("/api/workflowInstances")
class WorkflowInstanceController(
    private val workflowInstanceService: WorkflowInstanceService
) : BaseController() {

    @Operation(summary = "查询工作流实例列表")
    @GetMapping("/list")
    fun listWorkflowInstance(
        @Validated @NotNull param: WorkflowInstanceQueryRequestDTO?
    ) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.list(param!!)
    }

    @Operation(summary = "查询工作流实例详情")
    @GetMapping("/detail")
    fun getWorkflowInstance(@Validated @NotNull workflowInstanceId: Long?) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.get(workflowInstanceId!!)
    }

    @Operation(summary = "查询工作流节点实例进度")
    @GetMapping("/queryProgress")
    fun queryWorkflowNodeInstanceProgress(
        @Validated @NotNull param: WorkflowNodeInstanceProgressQueryRequestDTO?
    ) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.queryProgress(param!!)
    }

    @Operation(summary = "终止任务")
    @PostMapping("/terminate")
    fun terminateWorkflowInstance(@NotNull workflowInstanceId: Long?) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.terminate(workflowInstanceId!!)
    }

    @Operation(summary = "重跑任务")
    @PostMapping("/retry")
    fun retryWorkflowInstance(@NotNull workflowInstanceId: Long?) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.retry(workflowInstanceId!!)
    }
}