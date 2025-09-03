package tech.powerscheduler.server.application.actor.singleton

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.common.enums.ExecuteModeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.TaskTypeEnum
import tech.powerscheduler.server.application.service.JobInstanceService
import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.domainevent.DomainEventRepository
import tech.powerscheduler.server.domain.domainevent.DomainEventStatusEnum
import tech.powerscheduler.server.domain.domainevent.DomainEventTypeEnum.TASK_STATUS_CHANGED
import tech.powerscheduler.server.domain.job.JobInstance
import tech.powerscheduler.server.domain.job.JobInstanceId
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.task.Task
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.task.TaskStatusChangeEvent
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/6/18
 */
class TaskStatusChangeEventHandlerActor(
    context: ActorContext<Command>,
    val taskRepository: TaskRepository,
    val domainEventRepository: DomainEventRepository,
    val jobInstanceRepository: JobInstanceRepository,
    val transactionTemplate: TransactionTemplate,
    val jobInstanceService: JobInstanceService,
) : AbstractBehavior<TaskStatusChangeEventHandlerActor.Command>(context) {

    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface Command {
        object HandleEvent : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val domainEventRepository = applicationContext.getBean(DomainEventRepository::class.java)
            val taskRepository = applicationContext.getBean(TaskRepository::class.java)
            val jobInstanceRepository = applicationContext.getBean(JobInstanceRepository::class.java)
            val jobInstanceService = applicationContext.getBean(JobInstanceService::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = TaskStatusChangeEventHandlerActor(
                        context = context,
                        taskRepository = taskRepository,
                        domainEventRepository = domainEventRepository,
                        jobInstanceRepository = jobInstanceRepository,
                        transactionTemplate = transactionTemplate,
                        jobInstanceService = jobInstanceService,
                    )
                    timer.startTimerWithFixedDelay(
                        Command.HandleEvent,
                        Command.HandleEvent,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1),
                    )
                    return@withTimers actor
                }
            }.apply {
                Behaviors.supervise(this).onFailure(SupervisorStrategy.resume())
            }
        }
    }

    override fun createReceive(): Receive<Command?>? {
        return newReceiveBuilder()
            .onMessageEquals(Command.HandleEvent) { handleEvent() }
            .build()
    }

    fun handleEvent(): Behavior<Command> {
        var pageNo = 1
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val eventPage = domainEventRepository.findPendingList(
                eventType = TASK_STATUS_CHANGED,
                pageQuery = pageQuery,
            )
            if (eventPage.isEmpty()) {
                return this
            }
            val events = eventPage.content
            val aggregateId2eventGroup = events.groupBy { it.aggregateId }
            aggregateId2eventGroup.entries.forEach { (_, events) ->
                val firstEvent = events.first()
                val domainEventIdsToDelete = events.mapNotNull { it.id }.filterNot { it.value == firstEvent.id!!.value }
                domainEventRepository.deleteByIds(domainEventIdsToDelete)
                try {
                    domainEventRepository.save(firstEvent)
                    val event = JSON.readValue<TaskStatusChangeEvent>(firstEvent.body)!!
                    doHandleEvent(event)
                    firstEvent.eventStatus = DomainEventStatusEnum.SUCCESS
                    domainEventRepository.save(firstEvent)
                } catch (e: Exception) {
                    if (firstEvent.canRetry) {
                        firstEvent.resetStatusForRetry()
                    } else {
                        firstEvent.eventStatus = DomainEventStatusEnum.FAILED
                    }
                    domainEventRepository.save(firstEvent)
                    log.error("handle TaskStatusChangeEvent [{}] failed: {}", firstEvent.id!!.value, e.message, e)
                }
            }
        } while (eventPage.isNotEmpty())
        return this
    }

    private fun doHandleEvent(event: TaskStatusChangeEvent) {
        val jobInstanceId = JobInstanceId(event.jobInstanceId)
        val jobInstance = jobInstanceRepository.findById(jobInstanceId)!!
        val tasks = taskRepository.findAllByJobInstanceIdAndBatchAndTaskType(
            jobInstanceId = jobInstance.id!!,
            batch = jobInstance.batch!!,
        )
        val reduceTask = if (needCreateReduceTask(jobInstance, tasks)) {
            tasks.first().createReduceTask()
        } else {
            null
        }
        transactionTemplate.executeWithoutResult {
            if (reduceTask != null) {
                taskRepository.save(reduceTask)
            }
            jobInstanceService.updateJobInstanceProgress(jobInstanceId)
        }
    }

    fun needCreateReduceTask(
        jobInstance: JobInstance,
        tasks: List<Task>,
    ): Boolean {
        if (jobInstance.executeMode != ExecuteModeEnum.MAP_REDUCE) {
            return false
        }
        val existReduceTask = tasks.any { task -> task.taskType == TaskTypeEnum.REDUCE }
        if (existReduceTask) {
            return false
        }
        val allSubTaskList = tasks.filter { it.taskType == TaskTypeEnum.SUB }
        if (allSubTaskList.isEmpty()) {
            return false
        }
        return allSubTaskList.all { it.taskStatus == JobStatusEnum.SUCCESS }
    }
}
