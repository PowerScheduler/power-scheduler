package tech.powerscheduler.server.application.schedule.job

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
 * @since 2025/5/14
 */
class JobAssignorActor(
    context: ActorContext<Command>,
    private val jobScheduler: JobScheduler,
) : AbstractBehavior<JobAssignorActor.Command>(context) {

    sealed interface Command {
        object Assign : Command
        object ReassignAll : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val jobScheduler = applicationContext.getBean<JobScheduler>()
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = JobAssignorActor(
                        context = context,
                        jobScheduler = jobScheduler,
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
                jobScheduler.assignJobs()
                return@onMessageEquals this
            }
            .onMessageEquals(Command.ReassignAll) {
                jobScheduler.reassignJobs()
                return@onMessageEquals this
            }
            .build()
    }
}