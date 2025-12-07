package tech.powerscheduler.server.domain.workflowgroup

import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.namespace.Namespace

/**
 * @author grayrat
 * @since 2025/12/7
 */
interface WorkflowGroupRepository {

    fun pageQuery(query: WorkflowGroupQuery): Page<WorkflowGroup>

    fun findById(workflowGroupId: WorkflowGroupId): WorkflowGroup?

    fun findByNamespaceAndCode(namespace: Namespace, code: String): WorkflowGroup?

    fun save(workflowGroup: WorkflowGroup): WorkflowGroupId

}