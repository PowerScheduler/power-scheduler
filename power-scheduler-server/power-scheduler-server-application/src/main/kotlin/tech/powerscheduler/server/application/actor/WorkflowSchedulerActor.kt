package tech.powerscheduler.server.application.actor

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import tech.powerscheduler.server.application.scheduler.WorkflowScheduler
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/6/25
 */
class WorkflowSchedulerActor(
    context: ActorContext<Command>,
    private val workflowScheduler: WorkflowScheduler,
) : AbstractBehavior<WorkflowSchedulerActor.Command>(context) {

    sealed interface Command {
        object ScheduleWorkflows : Command
        object CreateTasks : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val workflowScheduler = applicationContext.getBean<WorkflowScheduler>()
            return Behaviors.setup { context ->
                return@setup Behaviors.withTimers { timer ->
                    timer.startTimerWithFixedDelay(
                        Command.ScheduleWorkflows,
                        Duration.ofSeconds(1)
                    )
                    timer.startTimerWithFixedDelay(
                        Command.CreateTasks,
                        Duration.ofSeconds(1)
                    )
                    val jobSchedulerActor = WorkflowSchedulerActor(
                        context = context,
                        workflowScheduler = workflowScheduler,
                    )
                    return@withTimers jobSchedulerActor
                }
            }.apply {
                Behaviors.supervise(this).onFailure(SupervisorStrategy.resume())
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessageEquals(Command.ScheduleWorkflows) {
                workflowScheduler.handleScheduleWorkflows()
                return@onMessageEquals this
            }
            .onMessageEquals(Command.CreateTasks) {
                workflowScheduler.handleCreateTasks()
                return@onMessageEquals this
            }
            .onSignal(PostStop::class.java) {
                workflowScheduler.resetWorkflowScheduler()
                return@onSignal this
            }
            .build()
    }
}