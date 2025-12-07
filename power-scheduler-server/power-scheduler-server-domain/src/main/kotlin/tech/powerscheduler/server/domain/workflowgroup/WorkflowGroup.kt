package tech.powerscheduler.server.domain.workflowgroup

import tech.powerscheduler.server.domain.namespace.Namespace
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/12/7
 */
class WorkflowGroup {
    /**
     * 命名空间
     */
    var namespace: Namespace? = null

    /**
     * 应用分组id
     */
    var id: WorkflowGroupId? = null

    /**
     * 应用编码
     */
    var code: String? = null

    /**
     * 应用分组名称
     */
    var name: String? = null

    /**
     * 创建人
     */
    var createdBy: String? = null

    /**
     * 创建时间
     */
    var createdAt: LocalDateTime? = null

    /**
     * 修改人
     */
    var updatedBy: String? = null

    /**
     * 修改时间
     */
    var updatedAt: LocalDateTime? = null
}