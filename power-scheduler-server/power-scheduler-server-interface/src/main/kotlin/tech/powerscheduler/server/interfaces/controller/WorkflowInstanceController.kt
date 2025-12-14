package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
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
@RequestMapping(WORKFLOW_INSTANCE_API)
class WorkflowInstanceController(
    private val workflowInstanceService: WorkflowInstanceService
) : BaseController() {

    @Operation(summary = "查询工作流实例列表")
    @GetMapping("/")
    fun listWorkflowInstance(
        @Validated param: WorkflowInstanceQueryRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.list(param)
    }

    @Operation(summary = "查询工作流实例详情")
    @GetMapping("/{workflowInstanceId}")
    fun getWorkflowInstance(
        @PathVariable workflowInstanceId: Long
    ) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.get(workflowInstanceId)
    }

    @Operation(summary = "查询工作流节点实例进度")
    @GetMapping("/{workflowInstanceId}/progress")
    fun queryWorkflowNodeInstanceProgress(
        @PathVariable workflowInstanceId: Long,
        @Validated param: WorkflowNodeInstanceProgressQueryRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.queryProgress(
            workflowInstanceId = workflowInstanceId,
            param = param
        )
    }

    @Operation(summary = "终止任务")
    @PostMapping("/{workflowInstanceId}/terminate")
    fun terminateWorkflowInstance(
        @PathVariable workflowInstanceId: Long,
    ) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.terminate(workflowInstanceId)
    }

    @Operation(summary = "重跑任务")
    @PostMapping("/{workflowInstanceId}/retry")
    fun retryWorkflowInstance(
        @PathVariable workflowInstanceId: Long,
    ) = wrapperResponse {
        return@wrapperResponse workflowInstanceService.retry(workflowInstanceId)
    }
}