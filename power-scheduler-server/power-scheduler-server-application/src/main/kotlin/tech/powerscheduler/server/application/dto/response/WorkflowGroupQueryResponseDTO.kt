package tech.powerscheduler.server.application.dto.response

import java.time.LocalDateTime

/**
 * 工作流分组查询响应
 *
 * @author grayrat
 * @since 2025/12/7
 */
class WorkflowGroupQueryResponseDTO {
    /**
     * 工作流分组id
     */
    var id: Long? = null

    /**
     * 命名空间编码
     */
    var namespaceCode: String? = null

    /**
     * 工作流分组编码
     */
    var code: String? = null

    /**
     * 工作流分组名称
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
}