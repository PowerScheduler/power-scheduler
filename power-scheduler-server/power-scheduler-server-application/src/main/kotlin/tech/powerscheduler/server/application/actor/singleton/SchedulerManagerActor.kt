package tech.powerscheduler.server.application.actor.singleton

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/7/11
 */
class SchedulerManagerActor(
    context: ActorContext<Command>,
    private val schedulerRepository: SchedulerRepository,
    private val jobInfoRepository: JobInfoRepository,
    private val workflowRepository: WorkflowRepository,
    private val transactionTemplate: TransactionTemplate,
    private val jobAssignorActorRef: ActorRef<JobAssignorActor.Command>,
    private val workflowAssignorActorRef: ActorRef<WorkflowAssignorActor.Command>
) : AbstractBehavior<SchedulerManagerActor.Command>(context) {

    private val schedulerAddressSet = mutableSetOf<String>()

    sealed interface Command {
        object AwareSchedulerChange : Command

        object CleanOfflineScheduler : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
            jobAssignorActorRef: ActorRef<JobAssignorActor.Command>,
            workflowAssignorActorRef: ActorRef<WorkflowAssignorActor.Command>,
        ): Behavior<Command> {
            val schedulerRepository = applicationContext.getBean(SchedulerRepository::class.java)
            val jobInfoRepository = applicationContext.getBean(JobInfoRepository::class.java)
            val workflowRepository = applicationContext.getBean(WorkflowRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = SchedulerManagerActor(
                        context = context,
                        schedulerRepository = schedulerRepository,
                        jobInfoRepository = jobInfoRepository,
                        workflowRepository = workflowRepository,
                        transactionTemplate = transactionTemplate,
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
            .onMessageEquals(Command.CleanOfflineScheduler) { cleanOfflineScheduler() }
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

    fun cleanOfflineScheduler(): Behavior<Command> {
        val expiredSchedulers = schedulerRepository.findAllExpired()
        expiredSchedulers.forEach { expiredScheduler ->
            transactionTemplate.executeWithoutResult {
                val scheduler = schedulerRepository.lockById(expiredScheduler.id!!)
                if (scheduler == null || scheduler.expired.not()) {
                    return@executeWithoutResult
                }
                // TODO: 临时方案, 后面再优化
                // 这样实现不靠谱, 因为现在的任务仍然在运行, 上报结果时可能会有更新操作, 如果刚好并发执行到了这里的话,
                // 刚清除scheduler字段可能会被重新更新回去了, 最好上锁再逐个处理
                jobInfoRepository.clearSchedulerByAddress(scheduler.address!!)
                workflowRepository.clearSchedulerByAddress(scheduler.address!!)
                schedulerRepository.remove(expiredScheduler.id!!)
            }
        }
        return this
    }
}