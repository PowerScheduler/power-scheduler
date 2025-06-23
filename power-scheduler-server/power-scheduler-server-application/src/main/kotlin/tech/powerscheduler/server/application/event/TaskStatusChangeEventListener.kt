package tech.powerscheduler.server.application.event

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tech.powerscheduler.common.enums.ExecuteModeEnum.*
import tech.powerscheduler.server.application.service.JobInstanceService
import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.domain.domainevent.AggregateTypeEnum
import tech.powerscheduler.server.domain.domainevent.DomainEvent
import tech.powerscheduler.server.domain.domainevent.DomainEventRepository
import tech.powerscheduler.server.domain.domainevent.DomainEventTypeEnum
import tech.powerscheduler.server.domain.job.JobInstanceId
import tech.powerscheduler.server.domain.task.TaskStatusChangeEvent

/**
 * @author grayrat
 * @since 2025/6/8
 */
@Component
class TaskStatusChangeEventListener(
    private val domainEventRepository: DomainEventRepository,
    private val jobInstanceService: JobInstanceService
) {

    @EventListener
    fun onTaskStatusChange(event: TaskStatusChangeEvent) {
        when (event.executeMode) {
            SINGLE -> updateJobInstanceProgressNow(event)
            BROADCAST, MAP, MAP_REDUCE -> persistentEvent(event)
        }
    }

    fun persistentEvent(event: TaskStatusChangeEvent) {
        val domainEvent = DomainEvent.create(
            aggregateId = event.jobInstanceId.toString(),
            aggregateType = AggregateTypeEnum.JOB_INSTANCE,
            eventType = DomainEventTypeEnum.TASK_STATUS_CHANGED,
            body = JSON.writeValueAsString(event),
        )
        domainEventRepository.save(domainEvent)
    }

    fun updateJobInstanceProgressNow(event: TaskStatusChangeEvent) {
        val jobInstanceId = JobInstanceId(event.jobInstanceId)
        jobInstanceService.updateJobInstanceProgress(jobInstanceId)
    }
}