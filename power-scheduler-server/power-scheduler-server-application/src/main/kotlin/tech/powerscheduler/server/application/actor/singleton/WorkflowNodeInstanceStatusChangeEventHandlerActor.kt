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
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.domainevent.DomainEventRepository
import tech.powerscheduler.server.domain.domainevent.DomainEventStatusEnum
import tech.powerscheduler.server.domain.domainevent.DomainEventTypeEnum
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceId
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstanceStatusChangeEvent
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/6/25
 */
class WorkflowNodeInstanceStatusChangeEventHandlerActor(
    context: ActorContext<Command>,
    private val domainEventRepository: DomainEventRepository,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workflowInstanceRepository: WorkflowInstanceRepository,
    private val transactionTemplate: TransactionTemplate,
) : AbstractBehavior<WorkflowNodeInstanceStatusChangeEventHandlerActor.Command>(context) {

    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface Command {
        object HandleEvent : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val domainEventRepository = applicationContext.getBean(DomainEventRepository::class.java)
            val jobInstanceRepository = applicationContext.getBean(JobInstanceRepository::class.java)
            val workflowInstanceRepository = applicationContext.getBean(WorkflowInstanceRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = WorkflowNodeInstanceStatusChangeEventHandlerActor(
                        context = context,
                        domainEventRepository = domainEventRepository,
                        jobInstanceRepository = jobInstanceRepository,
                        workflowInstanceRepository = workflowInstanceRepository,
                        transactionTemplate = transactionTemplate,
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
                eventType = DomainEventTypeEnum.WORKFLOW_NODE_INSTANCE_STATUS_CHANGED,
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
                    firstEvent.eventStatus = DomainEventStatusEnum.PROCESSING
                    domainEventRepository.save(firstEvent)
                    val event = JSON.readValue<WorkflowNodeInstanceStatusChangeEvent>(firstEvent.body)!!
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

    private fun doHandleEvent(event: WorkflowNodeInstanceStatusChangeEvent) {
        val workflowInstanceId = WorkflowInstanceId(event.workflowInstanceId)
        transactionTemplate.executeWithoutResult {
            val workflowInstance = workflowInstanceRepository.lockById(workflowInstanceId)
            if (workflowInstance == null) {
                return@executeWithoutResult
            }
            if (workflowInstance.status in WorkflowStatusEnum.COMPLETED_STATUSES) {
                return@executeWithoutResult
            }
            workflowInstance.updateProgress()
            if (workflowInstance.status == WorkflowStatusEnum.RUNNING) {
                val nextNodeInstances = workflowInstance.workflowNodeInstances
                    .filter { it.status == WorkflowStatusEnum.WAITING }
                    .filter { it.parents.all { parent -> parent.status == WorkflowStatusEnum.SUCCESS } }
                val jobInstances = nextNodeInstances.map { it.createJobInstance() }
                if (jobInstances.isNotEmpty()) {
                    jobInstanceRepository.saveAll(jobInstances)
                    log.info("nodeInstance {} is ready to run", nextNodeInstances.map { it.id!!.value })
                }
            }
            workflowInstanceRepository.save(workflowInstance)
        }
    }
}