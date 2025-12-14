package tech.powerscheduler.server.interfaces.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.powerscheduler.server.application.service.MetadataService

/**
 * @author grayrat
 * @since 2025/5/18
 */
@Tag(name = "MetadataApi")
@Validated
@RestController
@RequestMapping(METADATA_API)
internal class MetadataController(
    private val metadataService: MetadataService
) : BaseController() {

    @Operation(summary = "查询元数据")
    @GetMapping("/{metadataCodes}")
    fun listMetadata(@PathVariable vararg metadataCodes: String) = wrapperResponse {
        return@wrapperResponse metadataService.listMetadata(metadataCodes.toList())
    }

}