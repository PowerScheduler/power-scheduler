package tech.powerscheduler.server.application.utils

import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.enums.BaseEnum
import tech.powerscheduler.server.application.dto.response.EnumDTO
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

fun BaseEnum?.toDTO(): EnumDTO {
    if (this == null) {
        return EnumDTO()
    }
    return EnumDTO(
        code = this.code,
        label = this.label,
    )
}