package tech.powerscheduler.server.application.schedule.system

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import tech.powerscheduler.server.application.schedule.job.JobAssignorActor
import tech.powerscheduler.server.application.schedule.workflow.WorkflowAssignorActor
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/7/11
 */
class SchedulerManagerActor(
    context: ActorContext<Command>,
    private val schedulerRepository: SchedulerRepository,
    private val jobAssignorActorRef: ActorRef<JobAssignorActor.Command>,
    private val workflowAssignorActorRef: ActorRef<WorkflowAssignorActor.Command>
) : AbstractBehavior<SchedulerManagerActor.Command>(context) {

    private val schedulerAddressSet = mutableSetOf<String>()

    sealed interface Command {
        object AwareSchedulerChange : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
            jobAssignorActorRef: ActorRef<JobAssignorActor.Command>,
            workflowAssignorActorRef: ActorRef<WorkflowAssignorActor.Command>,
        ): Behavior<Command> {
            val schedulerRepository = applicationContext.getBean<SchedulerRepository>()
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = SchedulerManagerActor(
                        context = context,
                        schedulerRepository = schedulerRepository,
                        jobAssignorActorRef = jobAssignorActorRef,
                        workflowAssignorActorRef = workflowAssignorActorRef,
                    )
                    timer.startTimerWithFixedDelay(
                        Command.AwareSchedulerChange,
                        Command.AwareSchedulerChange,
                        Duration.ofSeconds(5),
                        Duration.ofHours(5),
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
            .onMessageEquals(Command.AwareSchedulerChange) { awareSchedulerChange() }
            .build()
    }

    fun awareSchedulerChange(): Behavior<Command> {
        val schedulers = schedulerRepository.findAll()
        val availableSchedulers = schedulers.filter { it.expired.not() }.mapNotNull { it.address }
        if (schedulerAddressSet.isEmpty()) {
            schedulerAddressSet.addAll(availableSchedulers)
            return this
        }
        if (schedulerAddressSet != availableSchedulers) {
            schedulerAddressSet.clear()
            schedulerAddressSet.addAll(availableSchedulers)
            jobAssignorActorRef.tell(JobAssignorActor.Command.ReassignAll)
            workflowAssignorActorRef.tell(WorkflowAssignorActor.Command.ReassignAll)
        }
        return this
    }

}