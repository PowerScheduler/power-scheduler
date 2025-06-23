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
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.scheduler.Scheduler
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import tech.powerscheduler.server.domain.workflow.WorkflowId
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/7/11
 */
class WorkflowAssignorActor(
    context: ActorContext<Command>,
    private val workflowRepository: WorkflowRepository,
    private val schedulerRepository: SchedulerRepository,
    private val transactionTemplate: TransactionTemplate,
) : AbstractBehavior<WorkflowAssignorActor.Command>(context) {

    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface Command {
        object Assign : Command

        object ReassignAll : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val workflowRepository = applicationContext.getBean(WorkflowRepository::class.java)
            val schedulerRepository = applicationContext.getBean(SchedulerRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = WorkflowAssignorActor(
                        context = context,
                        workflowRepository = workflowRepository,
                        transactionTemplate = transactionTemplate,
                        schedulerRepository = schedulerRepository,
                    )
                    timer.startTimerWithFixedDelay(
                        Command.Assign,
                        Command.Assign,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(3),
                    )
                    return@withTimers actor
                }
            }.apply {
                Behaviors.supervise(this).onFailure(SupervisorStrategy.resume())
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessageEquals(Command.Assign, this::handleAssign)
            .onMessageEquals(Command.ReassignAll, this::handleReassignAll)
            .build()
    }

    private fun handleAssign(): Behavior<Command> {
        val availableSchedulers = schedulerRepository.findAll().filter { it.expired.not() }
        if (availableSchedulers.isEmpty()) {
            log.error("handleAssign failed, no available schedulers")
            return this
        }
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 50)
            val page = workflowRepository.listAssignableIds(query)
            val workflowIds = page.content
            reassign(workflowIds, availableSchedulers)
        } while (page.isNotEmpty())
        return this
    }

    private fun handleReassignAll(): Behavior<Command> {
        val availableSchedulers = schedulerRepository.findAll().filter { it.expired.not() }
        if (availableSchedulers.isEmpty()) {
            log.error("handleReassignAll failed, no available schedulers")
            return this
        }
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 100)
            val page = workflowRepository.listAllIds(query)
            val workflowIds = page.content
            reassign(workflowIds, availableSchedulers)
        } while (page.isNotEmpty())
        return this
    }

    private fun reassign(workflowIds: List<WorkflowId>, availableSchedulers: List<Scheduler>) {
        if (workflowIds.isEmpty()) {
            return
        }
        workflowIds.forEachIndexed { index, workflowId ->
            try {
                transactionTemplate.executeWithoutResult {
                    val workflow = workflowRepository.lockById(workflowId)
                    if (workflow == null) {
                        return@executeWithoutResult
                    }
                    val schedulerIdx = workflowIds.size % availableSchedulers.size
                    val assignedScheduler = availableSchedulers[schedulerIdx]
                    workflow.schedulerAddress = assignedScheduler.address
                    workflowRepository.save(workflow)
                    log.info(
                        "assign workflow [{}] to server [{}]",
                        workflow.id!!.value,
                        workflow.schedulerAddress
                    )
                }
            } catch (e: Exception) {
                log.error("Failed to reassign workflow [{}]: {}", workflowId.value, e.message, e)
            }
        }
    }
}