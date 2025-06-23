package tech.powerscheduler.server.infrastructure.utils

import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.domain.workflow.*
import tech.powerscheduler.server.infrastructure.persistence.model.WorkflowInstanceEntity

/**
 * @author grayrat
 * @since 2025/7/9
 */
fun WorkflowInstance.toEntity(): WorkflowInstanceEntity {
    return WorkflowInstanceEntity().also {
        val workflowNodeInstance2entity = this.workflowNodeInstances.associateWith(WorkflowNodeInstance::toEntity)
        for (workflowNodeInstance in this.workflowNodeInstances) {
            val nodeInstanceEntity = workflowNodeInstance2entity[workflowNodeInstance]!!
            nodeInstanceEntity.workflowInstanceEntity = it
            nodeInstanceEntity.children = workflowNodeInstance.children
                .mapNotNullTo(mutableSetOf()) { child -> workflowNodeInstance2entity[child] }
        }
        it.appGroupEntity = this.appGroup!!.toEntity()
        it.workflowNodeInstanceEntities = workflowNodeInstance2entity.values.toSet()
        it.id = this.id?.value
        it.workflowId = this.workflowId!!.value
        it.code = this.code
        it.name = this.name
        it.status = this.status
        it.scheduleType = this.scheduleType
        it.scheduleAt = this.scheduleAt
        it.dataTime = this.dataTime
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.graphData = JSON.writeValueAsString(this.graphData!!)
    }
}

fun WorkflowInstanceEntity.toDomainModel(): WorkflowInstance {
    return WorkflowInstance().also {
        val nodeInstanceEntity2domainModel = this.workflowNodeInstanceEntities.associateWith { nodeInstanceEntity ->
            nodeInstanceEntity.toDomainModel()
        }
        nodeInstanceEntity2domainModel.forEach { nodeInstanceEntity, nodeInstance ->
            nodeInstance.workflowInstance = it
            nodeInstance.children = nodeInstanceEntity.children.mapNotNullTo(mutableSetOf()) { child ->
                nodeInstanceEntity2domainModel[child]
            }
            nodeInstance.parents = nodeInstanceEntity.parents.mapNotNullTo(mutableSetOf()) { parent ->
                nodeInstanceEntity2domainModel[parent]
            }
        }
        it.appGroup = this.appGroupEntity!!.toDomainModel()
        it.workflowNodeInstances = nodeInstanceEntity2domainModel.values.toList()
        it.id = WorkflowInstanceId(this.id!!)
        it.workflowId = WorkflowId(this.workflowId!!)
        it.code = this.code
        it.name = this.name
        it.status = this.status
        it.scheduleType = this.scheduleType
        it.scheduleAt = this.scheduleAt
        it.dataTime = this.dataTime
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.graphData = JSON.readValue<WorkflowInstanceGraphData>(this.graphData)
    }
}