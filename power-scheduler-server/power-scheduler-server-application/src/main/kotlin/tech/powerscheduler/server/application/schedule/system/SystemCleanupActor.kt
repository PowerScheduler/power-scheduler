package tech.powerscheduler.server.application.schedule.system

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
 * @since 2025/6/4
 */
class SystemCleanupActor(
    context: ActorContext<Command>,
    private val systemCleaner: SystemCleaner,
) : AbstractBehavior<SystemCleanupActor.Command>(context) {

    sealed interface Command {
        object CleanUpSystemResource : Command

        object CleanDueWorkerRegistry : Command
    }

    companion object {
        fun create(applicationContext: ApplicationContext): Behavior<Command> {
            val systemCleaner = applicationContext.getBean<SystemCleaner>()
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = SystemCleanupActor(
                        context = context,
                        systemCleaner = systemCleaner,
                    )
                    timer.startTimerWithFixedDelay(
                        Command.CleanUpSystemResource,
                        Command.CleanUpSystemResource,
                        Duration.ofSeconds(300),
                        Duration.ofHours(12),
                    )
                    timer.startTimerWithFixedDelay(
                        Command.CleanDueWorkerRegistry,
                        Command.CleanDueWorkerRegistry,
                        Duration.ofSeconds(0),
                        Duration.ofSeconds(1),
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
            .onMessageEquals(Command.CleanDueWorkerRegistry) {
                systemCleaner.handleCleanDueWorkerRegistry()
                return@onMessageEquals this
            }
            .onMessageEquals(Command.CleanUpSystemResource) {
                systemCleaner.cleanUp()
                return@onMessageEquals this
            }
            .build()
    }

}