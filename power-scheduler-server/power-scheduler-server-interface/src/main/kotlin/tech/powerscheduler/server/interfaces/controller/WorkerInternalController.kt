package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import tech.powerscheduler.common.api.*
import tech.powerscheduler.common.dto.request.*
import tech.powerscheduler.server.application.service.WorkerLifeCycleService

/**
 * @author grayrat
 * @since 2025/5/20
 */
@Hidden
@Tag(name = "worker-internal")
@Validated
@RestController
@RequestMapping(SERVER_API_PREFIX)
internal class WorkerInternalController(
    private val workerLifeCycleService: WorkerLifeCycleService,
) : BaseController() {

    @PostMapping(REGISTER_API)
    fun register(
        @RequestBody @NotNull param: WorkerRegisterRequestDTO?,
        httpServletRequest: HttpServletRequest,
    ) = wrapperResponse {
        return@wrapperResponse workerLifeCycleService.register(
            param = param!!,
            remoteHost = httpServletRequest.remoteAddr,
        )
    }

    @PostMapping(UNREGISTER_API)
    fun unregister(
        @RequestBody @NotNull param: WorkerUnregisterRequestDTO?,
        httpServletRequest: HttpServletRequest,
    ) = wrapperResponse {
        workerLifeCycleService.unregister(
            param = param!!,
            remoteAddr = httpServletRequest.remoteAddr,
        )
        return@wrapperResponse true
    }

    @PostMapping(HEARTBEAT_API)
    fun heartbeat(
        @RequestBody @NotNull param: WorkerHeartbeatRequestDTO?,
        httpServletRequest: HttpServletRequest,
    ) = wrapperResponse {
        workerLifeCycleService.handleHeartbeat(
            param = param!!,
            remoteAddr = httpServletRequest.remoteAddr,
        )
        return@wrapperResponse true
    }

    @GetMapping(CHECK_HEALTH_API)
    fun checkHealth() = wrapperResponse {
        return@wrapperResponse true
    }

    @PostMapping(REPORT_PROGRESS_API)
    fun reportProgress(@RequestBody @NotNull param: TaskProgressReportRequestDTO?) = wrapperResponse {
        workerLifeCycleService.updateProgress(param!!)
        return@wrapperResponse true
    }

    @PostMapping(REPORT_METRICS_API)
    fun reportMetrics(
        @RequestBody @NotNull param: WorkerMetricsReportRequestDTO?,
        httpServletRequest: HttpServletRequest
    ) = wrapperResponse {
        workerLifeCycleService.updateWorkerMetrics(
            param = param!!,
            remoteAddr = httpServletRequest.remoteAddr,
        )
        return@wrapperResponse true
    }

    @PostMapping(FETCH_TASK_RESULT)
    fun fetchTaskResult(
        @RequestBody @NotNull param: FetchTaskResultRequestDTO?,
    ) = wrapperResponse {
        val result = workerLifeCycleService.fetchTaskResult(param = param!!)
        return@wrapperResponse result
    }
}
