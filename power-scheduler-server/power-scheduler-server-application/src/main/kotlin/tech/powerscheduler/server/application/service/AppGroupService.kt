package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.AppGroupAssembler
import tech.powerscheduler.server.application.context.UserContext
import tech.powerscheduler.server.application.dto.request.AppGroupAddRequestDTO
import tech.powerscheduler.server.application.dto.request.AppGroupEditRequestDTO
import tech.powerscheduler.server.application.dto.request.AppGroupQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.AppGroupQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.appgroup.AppGroupId
import tech.powerscheduler.server.domain.appgroup.AppGroupRepository
import tech.powerscheduler.server.domain.namespace.NamespaceRepository

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Service
class AppGroupService(
    private val appGroupRepository: AppGroupRepository,
    private val namespaceRepository: NamespaceRepository,
    private val appGroupAssembler: AppGroupAssembler,
) {

    fun list(param: AppGroupQueryRequestDTO): PageDTO<AppGroupQueryResponseDTO> {
        val query = appGroupAssembler.toDomainQuery(param)
        val page = appGroupRepository.pageQuery(query)
        return page.toDTO().map { appGroupAssembler.toAppGroupQueryResponseDTO(it) }
    }

    fun add(param: AppGroupAddRequestDTO, userContext: UserContext): Long {
        val appCode = param.code!!
        val namespaceCode = param.namespaceCode!!
        val namespace = namespaceRepository.findByCode(namespaceCode)
            ?: throw BizException(message = "应用保存失败, 命名空间[${namespaceCode}]不存在")
        val existAppGroup = appGroupRepository.findByCode(namespace, appCode)
        if (existAppGroup != null) {
            throw BizException(message = "应用保存失败, 应用编码[${appCode}]已存在")
        }
        val appGroupToSave = appGroupAssembler.toDomainModel4AddRequest(param, namespace, userContext)
        val appGroupSaved = appGroupRepository.save(appGroupToSave)
        return appGroupSaved.id?.value!!
    }

    fun edit(
        appGroupId: Long,
        param: AppGroupEditRequestDTO,
        userContext: UserContext
    ) {
        val appGroupId = AppGroupId(appGroupId)
        val appGroup = appGroupRepository.findById(appGroupId)
            ?: throw BizException(message = "应用保存失败, 应用不存在")
        val appGroupToEdit = appGroupAssembler.toDomainModel4EditRequest(
            model = appGroup,
            param = param,
            userContext = userContext
        )
        appGroupRepository.save(appGroupToEdit)
    }

}