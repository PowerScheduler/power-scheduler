package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.powerscheduler.server.application.service.WorkflowNodeService

/**
 * @author grayrat
 * @since 2025/6/23
 */
@Deprecated(message = "废弃, 合并到WorkflowController")
@Tag(name = "WorkflowNodeApi")
@Validated
@RestController
@RequestMapping("/api/workflowNodes")
class WorkflowNodeController(
    private val workflowNodeService: WorkflowNodeService,
) : BaseController() {

}