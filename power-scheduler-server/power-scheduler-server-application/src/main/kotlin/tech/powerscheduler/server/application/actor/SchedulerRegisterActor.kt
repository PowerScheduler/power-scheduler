package tech.powerscheduler.server.application.actor

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.server.domain.scheduler.Scheduler
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import java.time.Duration
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/11
 */
class SchedulerRegisterActor(
    context: ActorContext<Command>,
    private val serverAddressHolder: ServerAddressHolder,
    private val schedulerRepository: SchedulerRepository,
    private val transactionTemplate: TransactionTemplate,
) : AbstractBehavior<SchedulerRegisterActor.Command>(context) {

    sealed interface Command {
        object Register : Command
    }

    companion object {
        fun create(applicationContext: ApplicationContext): Behavior<Command> {
            val serverAddressHolder = applicationContext.getBean(ServerAddressHolder::class.java)
            val schedulerRepository = applicationContext.getBean(SchedulerRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = SchedulerRegisterActor(
                        context = context,
                        serverAddressHolder = serverAddressHolder,
                        schedulerRepository = schedulerRepository,
                        transactionTemplate = transactionTemplate,
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
            .onMessageEquals(Command.Register) { register() }
            .onSignal(PostStop::class.java) { signal -> onPostStop() }
            .build()
    }

    fun register(): Behavior<Command> {
        val address = serverAddressHolder.address
        val existScheduler = schedulerRepository.findByAddress(address)
        transactionTemplate.execute {
            if (existScheduler != null) {
                schedulerRepository.lockById(existScheduler.id!!)
            }
            val schedulerToSave = Scheduler().apply {
                this.id = existScheduler?.id
                this.online = true
                this.address = address
                this.lastHeartbeatAt = LocalDateTime.now()
            }
            schedulerRepository.save(schedulerToSave)
        }
        return this
    }

    fun onPostStop(): Behavior<Command> {
        val address = serverAddressHolder.address
        val existScheduler = schedulerRepository.findByAddress(address)
        if (existScheduler != null) {
            existScheduler.online = false
            schedulerRepository.save(existScheduler)
        }
        return this
    }
}