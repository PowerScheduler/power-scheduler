package tech.powerscheduler.server.infrastructure.utils

import tech.powerscheduler.server.domain.appgroup.AppGroup
import tech.powerscheduler.server.domain.appgroup.AppGroupId
import tech.powerscheduler.server.domain.common.Page
import tech.powerscheduler.server.domain.domainevent.DomainEvent
import tech.powerscheduler.server.domain.domainevent.DomainEventId
import tech.powerscheduler.server.domain.job.*
import tech.powerscheduler.server.domain.namespace.Namespace
import tech.powerscheduler.server.domain.namespace.NamespaceId
import tech.powerscheduler.server.domain.task.Task
import tech.powerscheduler.server.domain.task.TaskId
import tech.powerscheduler.server.domain.worker.WorkerRegistry
import tech.powerscheduler.server.domain.worker.WorkerRegistryId
import tech.powerscheduler.server.infrastructure.persistence.model.*

/**
 * @author grayrat
 * @since 2025/4/17
 */
fun <T> org.springframework.data.domain.Page<T>.toDomainPage(): Page<T> {
    return Page(
        number = this.number + 1,
        size = this.size,
        totalPages = this.totalPages,
        totalElements = this.totalElements,
        content = this.content,
    )
}

fun NamespaceEntity.toDomainModel(): Namespace {
    return Namespace().also {
        it.id = NamespaceId(this.id!!)
        it.code = this.code
        it.name = this.name
        it.description = this.description
        it.createdBy = this.createdBy
        it.createdAt = this.createdAt
    }
}

fun Namespace.toEntity(): NamespaceEntity {
    return NamespaceEntity().also {
        it.id = this.id?.value
        it.code = this.code
        it.name = this.name
        it.description = this.description
    }
}

fun AppGroupEntity.toDomainModel(): AppGroup {
    return AppGroup().also {
        it.namespace = this.namespaceEntity!!.toDomainModel()
        it.id = AppGroupId(this.id!!)
        it.code = this.code
        it.name = this.name
        it.secret = this.secret
        it.createdAt = this.createdAt
        it.createdBy = this.createdBy
        it.updatedAt = this.updatedAt
        it.updatedBy = this.updatedBy
    }
}

fun AppGroup.toEntity(): AppGroupEntity {
    return AppGroupEntity().also {
        it.namespaceEntity = this.namespace?.toEntity()
        it.id = this.id?.value
        it.name = this.name
        it.code = this.code
        it.secret = this.secret
    }
}

fun JobInfoEntity.toDomainModel(): JobInfo {
    return JobInfo().also {
        it.appGroup = this.appGroupEntity!!.toDomainModel()
        it.id = JobId(this.id!!)
        it.jobName = this.jobName
        it.jobDesc = this.jobDesc
        it.jobType = this.jobType
        it.scheduleType = this.scheduleType
        it.scheduleConfig = this.scheduleConfig
        it.processor = this.processor
        it.executeMode = this.executeMode
        it.executeParams = this.executeParams
        it.nextScheduleAt = this.nextScheduleAt
        it.enabled = this.enabled
        it.maxConcurrentNum = this.maxConcurrentNum
        it.scheduleType = this.scheduleType
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.maxAttemptCnt = this.maxAttemptCnt
        it.attemptInterval = this.attemptInterval
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
        it.lastCompletedAt = this.lastCompletedAt
        it.schedulerAddress = this.schedulerAddress
        it.retentionPolicy = this.retentionPolicy
        it.retentionValue = this.retentionValue

        it.createdBy = this.createdBy
        it.createdAt = this.createdAt
        it.updatedBy = this.updatedBy
        it.updatedAt = this.updatedAt
    }
}

fun JobInfo.toEntity(): JobInfoEntity {
    return JobInfoEntity().also {
        it.appGroupEntity = this.appGroup?.toEntity()
        it.id = this.id?.value
        it.jobName = this.jobName
        it.jobDesc = this.jobDesc
        it.jobType = this.jobType
        it.scheduleType = this.scheduleType
        it.scheduleConfig = this.scheduleConfig
        it.processor = this.processor
        it.executeMode = this.executeMode
        it.executeParams = this.executeParams
        it.nextScheduleAt = this.nextScheduleAt
        it.enabled = this.enabled
        it.maxConcurrentNum = this.maxConcurrentNum
        it.scheduleType = this.scheduleType
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.maxAttemptCnt = this.maxAttemptCnt
        it.attemptInterval = this.attemptInterval
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
        it.lastCompletedAt = this.lastCompletedAt
        it.schedulerAddress = this.schedulerAddress
        it.retentionPolicy = this.retentionPolicy
        it.retentionValue = this.retentionValue
    }
}

fun JobInstance.toEntity(): JobInstanceEntity {
    return JobInstanceEntity().also {
        it.appGroupEntity = this.appGroup?.toEntity()
        it.id = this.id?.value
        it.sourceId = this.sourceId?.value
        it.sourceType = this.sourceType
        it.workflowInstanceCode = this.workflowInstanceCode
        it.workflowNodeInstanceCode = this.workflowNodeInstanceCode
        it.workerAddress = this.workerAddress
        it.schedulerAddress = this.schedulerAddress
        it.jobName = this.jobName
        it.jobType = this.jobType
        it.processor = this.processor
        it.jobStatus = this.jobStatus
        it.scheduleAt = this.scheduleAt
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.executeParams = this.executeParams
        it.executeMode = this.executeMode
        it.scheduleType = this.scheduleType
        it.message = this.message
        it.dataTime = this.dataTime
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.attemptCnt = this.attemptCnt
        it.maxAttemptCnt = this.maxAttemptCnt
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
    }
}

fun JobInstanceEntity.toDomainModel(): JobInstance {
    return JobInstance().also {
        it.appGroup = this.appGroupEntity?.toDomainModel()
        it.id = JobInstanceId(this.id!!)
        it.sourceId = SourceId(this.sourceId!!)
        it.sourceType = this.sourceType
        it.workflowInstanceCode = this.workflowInstanceCode
        it.workflowNodeInstanceCode = this.workflowNodeInstanceCode
        it.workerAddress = this.workerAddress
        it.schedulerAddress = this.schedulerAddress
        it.jobName = this.jobName
        it.jobType = this.jobType
        it.processor = this.processor
        it.jobStatus = this.jobStatus
        it.scheduleAt = this.scheduleAt
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.executeParams = this.executeParams
        it.executeMode = this.executeMode
        it.scheduleType = this.scheduleType
        it.message = this.message
        it.dataTime = this.dataTime
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.attemptCnt = this.attemptCnt
        it.maxAttemptCnt = this.maxAttemptCnt
        it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
        it.taskAttemptInterval = this.taskAttemptInterval
        it.priority = this.priority
    }
}

fun WorkerRegistry.toEntity(): WorkerRegistryEntity {
    return WorkerRegistryEntity().also {
        it.id = this.id?.value
        it.appCode = this.appCode
        it.namespaceCode = this.namespaceCode
        it.host = this.host
        it.port = this.port
        it.lastHeartbeatAt = this.lastHeartbeatAt
        it.accessToken = this.accessToken
        it.version = this.version
        it.cpuUsagePercent = this.cpuUsagePercent
        it.memoryUsagePercent = this.memoryUsagePercent
    }
}

fun WorkerRegistryEntity.toDomainModel(): WorkerRegistry {
    return WorkerRegistry().also {
        it.id = WorkerRegistryId(this.id!!)
        it.appCode = this.appCode
        it.namespaceCode = this.namespaceCode
        it.host = this.host
        it.port = this.port
        it.accessToken = this.accessToken
        it.lastHeartbeatAt = this.lastHeartbeatAt
        it.version = this.version
        it.cpuUsagePercent = this.cpuUsagePercent
        it.memoryUsagePercent = this.memoryUsagePercent
    }
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity().also {
        it.appGroupEntity = this.appGroup!!.toEntity()
        it.id = this.id?.value
        it.parentId = this.parentId?.value
        it.sourceId = this.sourceId!!.value
        it.sourceType = this.sourceType
        it.jobInstanceId = this.jobInstanceId!!.value
        it.schedulerAddress = this.schedulerAddress
        it.workerAddress = this.workerAddress
        it.taskName = this.taskName
        it.jobType = this.jobType
        it.processor = this.processor
        it.taskStatus = this.taskStatus
        it.scheduleAt = this.scheduleAt
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.executeParams = this.executeParams
        it.executeMode = this.executeMode
        it.scheduleType = this.scheduleType
        it.result = this.result
        it.dataTime = this.dataTime
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.attemptCnt = this.attemptCnt
        it.maxAttemptCnt = this.maxAttemptCnt
        it.priority = this.priority
        it.batch = this.batch
        it.taskBody = this.taskBody
        it.taskType = this.taskType
    }
}

fun TaskEntity.toDomainModel(): Task {
    return Task().also {
        it.appGroup = this.appGroupEntity?.toDomainModel()
        it.id = TaskId(this.id!!)
        it.parentId = this.parentId?.let { parentId -> TaskId(parentId) }
        it.sourceId = SourceId(this.sourceId!!)
        it.sourceType = this.sourceType
        it.jobInstanceId = JobInstanceId(this.jobInstanceId!!)
        it.schedulerAddress = this.schedulerAddress
        it.workerAddress = this.workerAddress
        it.taskName = this.taskName
        it.jobType = this.jobType
        it.processor = this.processor
        it.taskStatus = this.taskStatus
        it.scheduleAt = this.scheduleAt
        it.startAt = this.startAt
        it.endAt = this.endAt
        it.executeParams = this.executeParams
        it.executeMode = this.executeMode
        it.scheduleType = this.scheduleType
        it.result = this.result
        it.dataTime = this.dataTime
        it.scriptType = this.scriptType
        it.scriptCode = this.scriptCode
        it.attemptCnt = this.attemptCnt
        it.maxAttemptCnt = this.maxAttemptCnt
        it.priority = this.priority
        it.batch = this.batch
        it.taskBody = this.taskBody
        it.taskType = this.taskType
    }
}

fun DomainEvent.toEntity(): DomainEventEntity {
    return DomainEventEntity().also {
        it.id = this.id?.value
        it.aggregateId = this.aggregateId
        it.aggregateType = this.aggregateType
        it.eventType = this.eventType
        it.eventStatus = this.eventStatus
        it.body = this.body
        it.retryCnt = this.retryCnt
    }
}

fun DomainEventEntity.toDomainModel(): DomainEvent {
    return DomainEvent().also {
        it.id = DomainEventId(this.id!!)
        it.aggregateId = this.aggregateId
        it.aggregateType = this.aggregateType
        it.eventType = this.eventType
        it.eventStatus = this.eventStatus
        it.body = this.body
        it.retryCnt = this.retryCnt
    }
}
