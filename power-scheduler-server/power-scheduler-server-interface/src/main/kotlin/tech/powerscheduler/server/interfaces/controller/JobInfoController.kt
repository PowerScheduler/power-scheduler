package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import tech.powerscheduler.common.enums.JobTypeEnum
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.dto.request.*
import tech.powerscheduler.server.application.service.JobInfoService
import tech.powerscheduler.server.application.service.JobInstanceService

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Tag(name = "JobApi")
@Validated
@RestController
@RequestMapping(JOB_API)
internal class JobInfoController(
    private val jobInfoService: JobInfoService,
    private val jobInstanceService: JobInstanceService,
) : BaseController() {

    @Operation(summary = "查询任务列表")
    @GetMapping("/")
    fun listJob(
        @Validated @NotNull param: JobInfoQueryRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse jobInfoService.query(param)
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/{jobId}")
    fun getJob(
        @PathVariable jobId: Long
    ) = wrapperResponse {
        return@wrapperResponse jobInfoService.detail(jobId)
    }

    @Operation(summary = "新增任务")
    @PostMapping("/")
    fun addJob(
        @Validated @RequestBody param: JobInfoAddRequestDTO
    ) = wrapperResponse {
        if (param.jobType != JobTypeEnum.SCRIPT && param.processor.isNullOrBlank()) {
            throw BizException("任务处理器不能为空")
        }
        return@wrapperResponse jobInfoService.add(param)
    }

    @Operation(summary = "编辑任务")
    @PutMapping("/{jobId}")
    fun editJob(
        @PathVariable @NotNull jobId: Long,
        @Validated @RequestBody param: JobInfoEditRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse jobInfoService.edit(jobId, param)
    }

    @Operation(summary = "修改任务启用状态")
    @PatchMapping("/{jobId}/status")
    fun switchJobStatus(
        @PathVariable @NotNull jobId: Long,
        @Validated @RequestBody param: JobSwitchRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse jobInfoService.switch(jobId, param)
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{jobId}")
    fun removeJob(
        @PathVariable @NotNull jobId: Long
    ) = wrapperResponse {
        return@wrapperResponse jobInfoService.remove(jobId)
    }

    @Operation(summary = "运行任务")
    @PostMapping("/{jobId}/instance")
    fun runJob(
        @PathVariable @NotNull jobId: Long,
        @RequestBody @Validated @NotNull param: JobRunRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse jobInstanceService.run(jobId, param)
    }
}