package tech.powerscheduler.server.application.schedule.workflow

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/7/11
 */
class WorkflowAssignorActor(
    context: ActorContext<Command>,
    private val workflowScheduler: WorkflowScheduler
) : AbstractBehavior<WorkflowAssignorActor.Command>(context) {

    sealed interface Command {
        object Assign : Command
        object ReassignAll : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val workflowScheduler = applicationContext.getBean<WorkflowScheduler>()
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = WorkflowAssignorActor(
                        context = context,
                        workflowScheduler = workflowScheduler,
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
            .onMessageEquals(Command.Assign) {
                workflowScheduler.assignWorkflows()
                return@onMessageEquals this
            }
            .onMessageEquals(Command.ReassignAll) {
                workflowScheduler.reassignWorkflows()
                return@onMessageEquals this
            }
            .build()
    }

}