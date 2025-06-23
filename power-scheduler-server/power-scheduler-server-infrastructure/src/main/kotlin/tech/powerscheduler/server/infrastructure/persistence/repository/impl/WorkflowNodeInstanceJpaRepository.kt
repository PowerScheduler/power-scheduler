package tech.powerscheduler.server.infrastructure.persistence.repository.impl

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowNodeInstanceEntity

/**
 * @author grayrat
 * @since 2025/6/22
 */
interface WorkflowNodeInstanceJpaRepository
    : JpaRepository<WorkflowNodeInstanceEntity, Long>, JpaSpecificationExecutor<WorkflowNodeInstanceEntity> {
}