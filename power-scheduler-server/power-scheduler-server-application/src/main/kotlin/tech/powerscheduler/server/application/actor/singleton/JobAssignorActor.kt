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
import tech.powerscheduler.server.domain.job.JobId
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.domain.scheduler.Scheduler
import tech.powerscheduler.server.domain.scheduler.SchedulerRepository
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/5/14
 */
class JobAssignorActor(
    context: ActorContext<Command>,
    private val jobInfoRepository: JobInfoRepository,
    private val schedulerRepository: SchedulerRepository,
    private val transactionTemplate: TransactionTemplate,
) : AbstractBehavior<JobAssignorActor.Command>(context) {

    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface Command {
        object Assign : Command

        object ReassignAll : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val jobInfoRepository = applicationContext.getBean(JobInfoRepository::class.java)
            val schedulerRepository = applicationContext.getBean(SchedulerRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            return Behaviors.setup { context ->
                Behaviors.withTimers { timer ->
                    val actor = JobAssignorActor(
                        context = context,
                        jobInfoRepository = jobInfoRepository,
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
            val query = PageQuery(pageNo = pageNo++, pageSize = 200)
            val page = jobInfoRepository.listAssignableIds(query)
            val jobIds = page.content
            reassign(jobIds, availableSchedulers)
        } while (page.isNotEmpty())
        return this
    }

    private fun handleReassignAll(): Behavior<Command> {
        val availableSchedulers = schedulerRepository.findAll().filter { it.expired.not() }
        if (availableSchedulers.isEmpty()) {
            log.error("handleReassignAllJob failed, no available schedulers")
            return this
        }
        var pageNo = 1
        do {
            val query = PageQuery(pageNo = pageNo++, pageSize = 1000)
            val page = jobInfoRepository.listAllIds(query)
            val jobIds = page.content
            reassign(jobIds, availableSchedulers)
        } while (page.isNotEmpty())
        return this
    }

    private fun reassign(jobIds: List<JobId>, availableSchedulers: List<Scheduler>) {
        if (jobIds.isEmpty()) {
            return
        }
        jobIds.forEachIndexed { index, jobId ->
            try {
                transactionTemplate.executeWithoutResult {
                    val jobInfo = jobInfoRepository.lockById(jobId)
                    if (jobInfo == null) {
                        return@executeWithoutResult
                    }
                    val schedulerIdx = jobIds.size % availableSchedulers.size
                    val assignedScheduler = availableSchedulers[schedulerIdx]
                    jobInfo.schedulerAddress = assignedScheduler.address
                    jobInfoRepository.save(jobInfo)
                    log.info("assign job [{}] to server [{}]", jobInfo.id!!.value, jobInfo.schedulerAddress)
                }
            } catch (e: Exception) {
                log.error("Failed to reassign job [{}]: {}", jobId.value, e.message, e)
            }
        }
    }
}