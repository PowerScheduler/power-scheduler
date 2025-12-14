package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.powerscheduler.server.application.dto.request.WorkerQueryRequestDTO
import tech.powerscheduler.server.application.service.WorkerLifeCycleService

/**
 * @author grayrat
 * @since 2025/5/30
 */
@Tag(name = "WorkerApi")
@Validated
@RestController
@RequestMapping(WORKER_API)
class WorkerController(
    private val workerLifeCycleService: WorkerLifeCycleService,
) : BaseController() {

    @GetMapping("/")
    fun listWorker(
        @Validated param: WorkerQueryRequestDTO
    ) = wrapperResponse {
        return@wrapperResponse workerLifeCycleService.list(param)
    }
}