package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import tech.powerscheduler.common.annotation.Metadata
import tech.powerscheduler.common.enums.*
import tech.powerscheduler.server.application.dto.response.MetadataDTO
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * @author grayrat
 * @since 2025/5/18
 */
@Service
class MetadataService {

    private val metadataEnumMap: Map<String, KClass<*>> = listOf(
        JobTypeEnum::class,
        JobStatusEnum::class,
        ScheduleTypeEnum::class,
        ExecuteModeEnum::class,
        ScriptTypeEnum::class,
        RetentionPolicyEnum::class,
        WorkflowStatusEnum::class,
    ).associateBy { it.simpleName!! }

    fun listMetadata(metadataCodes: List<String>): List<MetadataDTO> {
        return metadataCodes.mapNotNull { buildMetadata(it) }
    }

    fun buildMetadata(metadataCode: String): MetadataDTO? {
        val metadataClass = metadataEnumMap[metadataCode] ?: return null
        val metadataAnnotation = metadataClass.findAnnotation<Metadata>()!!
        val options = metadataClass.java.enumConstants.mapNotNull { buildOption(it as BaseEnum) }
        return MetadataDTO(
            code = metadataCode,
            label = metadataAnnotation.label,
            options = options
        )
    }

    private fun buildOption(option: BaseEnum): MetadataDTO? {
        return MetadataDTO(
            code = option.code,
            label = option.label,
        )
    }
}