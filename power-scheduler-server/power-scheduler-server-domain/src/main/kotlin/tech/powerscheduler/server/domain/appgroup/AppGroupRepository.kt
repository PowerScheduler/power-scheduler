package tech.powerscheduler.server.domain.appgroup

import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.namespace.Namespace

/**
 * 应用分组持久化仓库
 *
 * @author grayrat
 * @since 2025/4/16
 */
interface AppGroupRepository {

    fun pageQuery(query: AppGroupQuery): Page<AppGroup>

    fun findById(appGroupId: AppGroupId): AppGroup?

    fun findByCode(namespace: Namespace, code: String): AppGroup?

    fun save(model: AppGroup): AppGroup

    fun findByCodes(appCodes: Collection<String>): List<AppGroup>

}