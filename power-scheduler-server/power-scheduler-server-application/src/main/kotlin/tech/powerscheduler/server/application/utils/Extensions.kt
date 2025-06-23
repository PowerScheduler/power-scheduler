package tech.powerscheduler.server.application.utils

import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.enums.*
import tech.powerscheduler.server.application.dto.response.*
import tech.powerscheduler.server.domain.common.Page

/**
 * @author grayrat
 * @since 2025/4/18
 */
fun <T> Page<T>.toDTO(): PageDTO<T> {
    return PageDTO(
        number = this.number,
        size = this.size,
        totalElements = this.totalElements,
        totalPages = this.totalPages,
        content = this.content
    )
}

fun ScheduleTypeEnum?.toDTO(): ScheduleTypeDTO {
    if (this == null) {
        return ScheduleTypeDTO()
    }
    return ScheduleTypeDTO(
        code = this.code,
        label = this.label,
    )
}

fun JobTypeEnum?.toDTO(): JobTypeDTO {
    if (this == null) {
        return JobTypeDTO()
    }
    return JobTypeDTO(
        code = this.code,
        label = this.label,
    )
}

fun ExecuteModeEnum?.toDTO(): ExecuteModeDTO {
    if (this == null) {
        return ExecuteModeDTO()
    }
    return ExecuteModeDTO(
        code = this.code,
        label = this.label,
    )
}

fun ScriptTypeEnum?.toDTO(): ScriptTypeDTO {
    if (this == null) {
        return ScriptTypeDTO()
    }
    return ScriptTypeDTO(
        code = this.code,
        label = this.label,
    )
}

fun JobStatusEnum?.toDTO(): JobStatusDTO {
    if (this == null) {
        return JobStatusDTO()
    }
    return JobStatusDTO(
        code = this.code,
        label = this.label,
    )
}

fun RetentionPolicyEnum?.toDTO(): RetentionPolicyDTO {
    if (this == null) {
        return RetentionPolicyDTO()
    }
    return RetentionPolicyDTO(
        code = this.code,
        label = this.label,
    )
}

fun WorkflowStatusEnum?.toDTO(): WorkflowStatusDTO {
    if (this == null) {
        return WorkflowStatusDTO()
    }
    return WorkflowStatusDTO(
        code = this.code,
        label = this.label,
    )
}