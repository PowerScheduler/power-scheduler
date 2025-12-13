package tech.powerscheduler.server.application.schedule.system

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import tech.powerscheduler.server.application.service.SchedulerService
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/7/11
 */
class SchedulerRegisterActor(
    context: ActorContext<Command>,
    private val schedulerService: SchedulerService,
) : AbstractBehavior<SchedulerRegisterActor.Command>(context) {

    sealed interface Command {
        object Register : Command
    }

    companion object {
        fun create(applicationContext: ApplicationContext): Behavior<Command> {
            val schedulerService = applicationContext.getBean<SchedulerService>()
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = SchedulerRegisterActor(
                        context = context,
                        schedulerService = schedulerService,
                    )
                    timer.startTimerWithFixedDelay(
                        Command.Register,
                        Command.Register,
                        Duration.ofSeconds(0),
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
            .onMessageEquals(Command.Register) {
                schedulerService.registerCurrentScheduler()
                return@onMessageEquals this
            }
            .onSignal(PostStop::class.java) { _ ->
                schedulerService.removeCurrentScheduler()
                return@onSignal this
            }
            .build()
    }
}