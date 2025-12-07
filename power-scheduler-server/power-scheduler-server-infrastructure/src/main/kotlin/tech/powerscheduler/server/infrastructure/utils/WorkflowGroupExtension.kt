package tech.powerscheduler.server.infrastructure.utils

import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroup
import tech.powerscheduler.server.domain.workflowgroup.WorkflowGroupId
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowGroupEntity

/**
 * @author grayrat
 * @since 2025/12/7
 */
fun WorkflowGroupEntity.toDomainModel(): WorkflowGroup {
    return WorkflowGroup().also {
        it.namespace = this.namespaceEntity!!.toDomainModel()
        it.id = WorkflowGroupId(this.id!!)
        it.code = this.code
        it.name = this.name
        it.createdBy = this.createdBy
        it.createdAt = this.createdAt
        it.updatedBy = this.updatedBy
        it.updatedAt = this.updatedAt
    }
}

fun WorkflowGroup.toEntity(): WorkflowGroupEntity {
    return WorkflowGroupEntity().also {
        it.namespaceEntity = this.namespace!!.toEntity()
        it.id = this.id?.value
        it.code = this.code
        it.name = this.name
        it.createdBy = this.createdBy
        it.createdAt = this.createdAt
        it.updatedBy = this.updatedBy
        it.updatedAt = this.updatedAt
    }
}