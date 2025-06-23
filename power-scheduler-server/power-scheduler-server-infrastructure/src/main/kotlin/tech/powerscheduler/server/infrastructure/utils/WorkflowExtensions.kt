package tech.powerscheduler.server.infrastructure.utils

import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.domain.workflow.Workflow
import tech.powerscheduler.server.domain.workflow.WorkflowGraphData
import tech.powerscheduler.server.domain.workflow.WorkflowId
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowEntity
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowNodeEntity

/**
 * @author grayrat
 * @since 2025/7/9
 */
fun Workflow.toEntity(): WorkflowEntity {
    return WorkflowEntity().also {
        it.appGroupEntity = this.appGroup!!.toEntity()
        val workflowNodeDomainModel2entity = this.workflowNodes.associateWith { node -> node.toEntity() }
        for (workflowNode in this.workflowNodes) {
            val workflowNodeEntity = workflowNodeDomainModel2entity[workflowNode]!!
            workflowNodeEntity.workflowEntity = it
            workflowNodeEntity.children = workflowNode.children
                .mapNotNull { entity -> workflowNodeDomainModel2entity[entity] }
                .toSet()
        }
        it.workflowNodeEntities = workflowNodeDomainModel2entity.values.toSet()

        it.id = this.id?.value
        it.name = this.name
        it.description = this.description
        it.graphData = JSON.writeValueAsString(this.graphData!!)
        it.scheduleType = this.scheduleType
        it.scheduleConfig = this.scheduleConfig
        it.nextScheduleAt = this.nextScheduleAt
        it.enabled = this.enabled
        it.schedulerAddress = this.schedulerAddress
        it.maxConcurrentNum = this.maxConcurrentNum
        it.lastCompletedAt = this.lastCompletedAt
        it.retentionPolicy = this.retentionPolicy
        it.retentionValue = this.retentionValue
    }
}

fun WorkflowEntity.toDomainModel(): Workflow {
    return Workflow().also {
        val workflowNodeEntity2Model = this.workflowNodeEntities.associateWith(WorkflowNodeEntity::toDomainModel)
        for (workflowNodeEntity in this.workflowNodeEntities) {
            val workflowNode = workflowNodeEntity2Model[workflowNodeEntity]!!
            workflowNode.workflow = it
            workflowNode.children = workflowNodeEntity.children
                .mapNotNull { entity -> workflowNodeEntity2Model[entity] }
                .toSet()
            workflowNode.parents = workflowNodeEntity.parents
                .mapNotNull { entity -> workflowNodeEntity2Model[entity] }
                .toSet()
        }

        it.appGroup = this.appGroupEntity!!.toDomainModel()
        it.workflowNodes = workflowNodeEntity2Model.values.toList()
        it.id = WorkflowId(this.id!!)
        it.name = this.name
        it.description = this.description
        it.graphData = JSON.readValue<WorkflowGraphData>(this.graphData)
        it.enabled = this.enabled
        it.schedulerAddress = this.schedulerAddress
        it.maxConcurrentNum = this.maxConcurrentNum
        it.retentionPolicy = this.retentionPolicy
        it.retentionValue = this.retentionValue
        it.nextScheduleAt = this.nextScheduleAt
        it.scheduleType = this.scheduleType
        it.scheduleConfig = this.scheduleConfig
        it.lastCompletedAt = this.lastCompletedAt
        it.createdBy = this.createdBy
        it.createdAt = this.createdAt
        it.updatedBy = this.updatedBy
        it.updatedAt = this.updatedAt
    }
}