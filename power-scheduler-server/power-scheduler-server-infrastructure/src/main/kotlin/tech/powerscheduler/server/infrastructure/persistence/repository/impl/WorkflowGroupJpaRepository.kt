package tech.powerscheduler.server.infrastructure.persistence.repository.impl

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.powerscheduler.server.infrastructure.persistence.model.NamespaceEntity
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowGroupEntity

/**
 * @author grayrat
 * @since 2025/12/7
 */
interface WorkflowGroupJpaRepository
    : JpaRepository<WorkflowGroupEntity, Long>, JpaSpecificationExecutor<WorkflowGroupEntity> {

    fun findByNamespaceEntityAndCode(namespaceEntity: NamespaceEntity, code: String): WorkflowGroupEntity?

}