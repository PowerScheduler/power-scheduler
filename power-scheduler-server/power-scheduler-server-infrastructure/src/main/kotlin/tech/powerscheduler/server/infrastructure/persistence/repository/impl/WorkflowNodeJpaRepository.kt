package tech.powerscheduler.server.infrastructure.persistence.repository.impl

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowEntity
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowNodeEntity

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowNodeJpaRepository
    : JpaRepository<WorkflowNodeEntity, Long>, JpaSpecificationExecutor<WorkflowNodeEntity> {

    fun findAllByWorkflowEntity(entity: WorkflowEntity): List<WorkflowNodeEntity>

}