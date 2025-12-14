package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.NamespaceAssembler
import tech.powerscheduler.server.application.dto.request.NamespaceAddRequestDTO
import tech.powerscheduler.server.application.dto.request.NamespaceEditRequestDTO
import tech.powerscheduler.server.application.dto.request.NamespaceQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.NamespaceQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.namespace.Namespace
import tech.powerscheduler.server.domain.namespace.NamespaceId
import tech.powerscheduler.server.domain.namespace.NamespaceRepository

/**
 * @author grayrat
 * @since 2025/6/21
 */
@Service
class NamespaceService(
    private val namespaceAssembler: NamespaceAssembler,
    private val namespaceRepository: NamespaceRepository,
) {

    fun query(param: NamespaceQueryRequestDTO): PageDTO<NamespaceQueryResponseDTO> {
        val domainQuery = namespaceAssembler.toDomainQuery(param)
        val page: Page<Namespace> = namespaceRepository.pageQuery(domainQuery)
        return page.toDTO().map { namespaceAssembler.toNamespaceQueryResponseDTO(it) }
    }

    fun add(param: NamespaceAddRequestDTO): Long {
        val namespaceToSave = namespaceAssembler.toDomainModel4AddRequestDTO(param)
        val namespaceId = namespaceRepository.save(namespaceToSave)
        return namespaceId.value
    }

    fun edit(namespaceId: Long, param: NamespaceEditRequestDTO) {
        val namespaceId = NamespaceId(namespaceId)
        val namespace = namespaceRepository.findById(namespaceId)
            ?: throw BizException("命名空间不存在")
        val namespaceToSave = namespaceAssembler.toDomainModel4EditRequestDTO(namespace, param)
        namespaceRepository.save(namespaceToSave)
    }
}