package tech.powerscheduler.server.application.schedule.job

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import java.time.Duration

class JobSchedulerActor(
    context: ActorContext<Command>,
    private val jobScheduler: JobScheduler,
) : AbstractBehavior<JobSchedulerActor.Command>(context) {

    sealed interface Command {
        object ScheduleJobs : Command
        object CreateTasks : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val jobScheduler = applicationContext.getBean<JobScheduler>()
            return Behaviors.setup { context ->
                return@setup Behaviors.withTimers { timer ->
                    timer.startTimerWithFixedDelay(
                        Command.ScheduleJobs,
                        Duration.ofSeconds(1)
                    )
                    timer.startTimerWithFixedDelay(
                        Command.CreateTasks,
                        Duration.ofSeconds(1)
                    )
                    val jobSchedulerActor = JobSchedulerActor(
                        context = context,
                        jobScheduler = jobScheduler,
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
            .onMessageEquals(Command.ScheduleJobs) {
                jobScheduler.handleScheduleDueJobs()
                return@onMessageEquals this
            }
            .onMessageEquals(Command.CreateTasks) {
                jobScheduler.handleCreateTasks()
                return@onMessageEquals this
            }
            .onSignal(PostStop::class.java) {
                jobScheduler.onPostStop()
                return@onSignal this
            }
            .build()
    }
}