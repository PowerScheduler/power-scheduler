package tech.powerscheduler.server.interfaces.controller


import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import tech.powerscheduler.server.application.dto.request.JobInstanceQueryRequestDTO
import tech.powerscheduler.server.application.dto.request.JobProgressQueryRequestDTO
import tech.powerscheduler.server.application.service.JobInstanceService

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Tag(name = "JobInstanceApi")
@Validated
@RestController
@RequestMapping("/api/jobInstances")
internal class JobInstanceController(
    private val jobInstanceService: JobInstanceService
) : BaseController() {

    @Operation(summary = "查询任务实例列表")
    @PostMapping("/list")
    fun listJobInstance(@Validated @RequestBody param: JobInstanceQueryRequestDTO) = wrapperResponse {
        val result = jobInstanceService.list(param)
        return@wrapperResponse result
    }

    @Operation(summary = "查询任务实例详情")
    @GetMapping("/detail")
    fun getJobInstanceDetail(jobInstanceId: Long) = wrapperResponse {
        jobInstanceService.detail(jobInstanceId)
    }

    @Operation(summary = "查询任务错误信息")
    @GetMapping("/getErrorMessage")
    fun getErrorMessage(jobInstanceId: Long) = wrapperResponse {
        jobInstanceService.getErrorMessage(jobInstanceId)
    }

    @Operation(summary = "终止任务")
    @PostMapping("/terminate")
    fun terminateJobInstance(jobInstanceId: Long) = wrapperResponse {
        jobInstanceService.terminate(jobInstanceId)
    }

    @Operation(summary = "重跑任务")
    @PostMapping("retry")
    fun retryJobInstance(@NotNull jobInstanceId: Long?) = wrapperResponse {
        jobInstanceService.retry(jobInstanceId!!)
    }

    @Operation(summary = "查询任务进度")
    @PostMapping("/queryProgress")
    fun queryProgress(@RequestBody @NotNull param: JobProgressQueryRequestDTO) = wrapperResponse {
        jobInstanceService.queryProgress(param)
    }
}
