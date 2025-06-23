package tech.powerscheduler.server.application.actor

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionTemplate
import tech.powerscheduler.common.enums.ExecuteModeEnum
import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.server.application.assembler.TaskAssembler
import tech.powerscheduler.server.domain.appgroup.AppGroupKey
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.domain.job.JobInstance
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.job.SourceId
import tech.powerscheduler.server.domain.task.Task
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.task.TaskStatusChangeEvent
import tech.powerscheduler.server.domain.worker.WorkerRegistry
import tech.powerscheduler.server.domain.worker.WorkerRegistryRepository
import tech.powerscheduler.server.domain.worker.WorkerRemoteService
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
import java.time.Duration

/**
 * @author grayrat
 * @since 2025/5/14
 */
class TaskDispatcherActor(
    context: ActorContext<Command>,
    private val taskAssembler: TaskAssembler,
    private val taskRepository: TaskRepository,
    private val jobInfoRepository: JobInfoRepository,
    private val workflowRepository: WorkflowRepository,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workerRegistryRepository: WorkerRegistryRepository,
    private val workerRemoteService: WorkerRemoteService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val transactionTemplate: TransactionTemplate,
    private val serverAddressHolder: ServerAddressHolder,
) : AbstractBehavior<TaskDispatcherActor.Command>(context) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    sealed interface Command {
        object DispatchJobTask : Command
        object DispatchWorkflowTask : Command
    }

    companion object {
        fun create(
            applicationContext: ApplicationContext,
        ): Behavior<Command> {
            val taskAssembler = applicationContext.getBean(TaskAssembler::class.java)
            val taskRepository = applicationContext.getBean(TaskRepository::class.java)
            val jobInstanceRepository = applicationContext.getBean(JobInstanceRepository::class.java)
            val workerRegistryRepository = applicationContext.getBean(WorkerRegistryRepository::class.java)
            val workerRemoteService = applicationContext.getBean(WorkerRemoteService::class.java)
            val jobInfoRepository = applicationContext.getBean(JobInfoRepository::class.java)
            val workflowRepository = applicationContext.getBean(WorkflowRepository::class.java)
            val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)
            val serverAddressHolder = applicationContext.getBean(ServerAddressHolder::class.java)
            val applicationEventPublisher = applicationContext as ApplicationEventPublisher
            return Behaviors.setup { context ->
                return@setup Behaviors.withTimers { timer ->
                    timer.startTimerWithFixedDelay(
                        Command.DispatchJobTask,
                        Duration.ofSeconds(1)
                    )
                    timer.startTimerWithFixedDelay(
                        Command.DispatchWorkflowTask,
                        Duration.ofSeconds(1)
                    )
                    val jobDispatcherActor = TaskDispatcherActor(
                        context = context,
                        taskAssembler = taskAssembler,
                        taskRepository = taskRepository,
                        jobInfoRepository = jobInfoRepository,
                        workflowRepository = workflowRepository,
                        jobInstanceRepository = jobInstanceRepository,
                        workerRegistryRepository = workerRegistryRepository,
                        workerRemoteService = workerRemoteService,
                        applicationEventPublisher = applicationEventPublisher,
                        transactionTemplate = transactionTemplate,
                        serverAddressHolder = serverAddressHolder,
                    )
                    return@withTimers jobDispatcherActor
                }
            }.apply {
                Behaviors.supervise(this).onFailure(SupervisorStrategy.resume())
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessageEquals(Command.DispatchJobTask) { this.handleDispatchJobTask() }
            .onMessageEquals(Command.DispatchWorkflowTask) { this.handleDispatchWorkflowTask() }
            .build()
    }

    private fun handleDispatchJobTask(): Behavior<Command> {
        var pageNo = 1
        val currentServerAddress = serverAddressHolder.address
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val assignedJobIdPage = jobInfoRepository.listIdsByEnabledAndSchedulerAddress(
                enabled = null,
                schedulerAddress = currentServerAddress,
                pageQuery = pageQuery,
            )
            val jobIds = assignedJobIdPage.content
            if (jobIds.isEmpty()) {
                continue
            }
            dispatch(
                sourceIds = jobIds.map { it.toSourceId() },
                sourceType = JobSourceTypeEnum.JOB
            )
            pageNo++
        } while (assignedJobIdPage.isNotEmpty())
        return this
    }

    private fun handleDispatchWorkflowTask(): Behavior<Command> {
        var pageNo = 1
        val currentServerAddress = serverAddressHolder.address
        do {
            val pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            val assignedJobIdPage = workflowRepository.listIdsByEnabledAndSchedulerAddress(
                enabled = null,
                schedulerAddress = currentServerAddress,
                pageQuery = pageQuery,
            )
            val workflowIds = assignedJobIdPage.content
            if (workflowIds.isEmpty()) {
                continue
            }
            dispatch(
                sourceIds = workflowIds.map { it.toSourceId() },
                sourceType = JobSourceTypeEnum.WORKFLOW
            )
            pageNo++
        } while (assignedJobIdPage.isNotEmpty())
        return this
    }

    private fun dispatch(sourceIds: List<SourceId>, sourceType: JobSourceTypeEnum) {
        var pageNo = 1
        do {
            val dispatchablePage = taskRepository.listDispatchable(
                sourceIds = sourceIds,
                sourceType = sourceType,
                pageQuery = PageQuery(pageNo = pageNo++, pageSize = 200)
            )
            val dispatchableJobInstanceList = dispatchablePage.content
            dispatch(dispatchableJobInstanceList)
        } while (dispatchablePage.isNotEmpty())
    }

    private fun dispatch(dispatchableList: List<Task>) {
        if (dispatchableList.isEmpty()) {
            return
        }
        val appGroupKey2DispatchableList = dispatchableList.groupBy { AppGroupKey(it.appGroup!!) }
        val appGroupKeys = appGroupKey2DispatchableList.keys
        val appGroupKey2WorkerRegistries = workerRegistryRepository.findAllByAppGroupKeys(appGroupKeys)

        // 使用管道限制最大并发数量, 为了避免并发大量的请求导致系统资源不足
        val channel = Channel<Unit>(20)
        runBlocking {
            val asyncDispatchJobs = dispatchableList.map { task ->
                async {
                    channel.send(Unit)
                    val appGroupKey = AppGroupKey(task.appGroup!!)
                    val candidateWorkers = appGroupKey2WorkerRegistries[appGroupKey].orEmpty().toSet()
                    try {
                        dispatchOne(task, candidateWorkers)
                    } catch (e: Exception) {
                        log.error("dispatch task [{}] failed: {}", task.id!!.value, e.message, e)
                    } finally {
                        channel.receive()
                    }
                }
            }
            asyncDispatchJobs.awaitAll()
        }
    }

    fun dispatchOne(task: Task, candidateWorkers: Set<WorkerRegistry>) {
        val jobInstance = jobInstanceRepository.findById(task.jobInstanceId!!)
        if (jobInstance == null) {
            log.warn("jobInstance with id [{}] not found", task.jobInstanceId?.value)
            return
        }
        if (candidateWorkers.isEmpty()) {
            if (task.canReattempt) {
                task.resetStatusForReattempt()
            } else {
                task.markFailed(message = "no available worker")
            }
            transactionTemplate.executeWithoutResult {
                taskRepository.save(task)
                publishTaskStatusChangeEvent(task)
            }
            return
        }

        val targetWorker = selectWorker(task, jobInstance, candidateWorkers)
        task.workerAddress = targetWorker
        task.schedulerAddress = serverAddressHolder.address
        task.taskStatus = JobStatusEnum.DISPATCHING
        transactionTemplate.executeWithoutResult {
            taskRepository.save(task)
            if (jobInstance.jobStatus == JobStatusEnum.WAITING_DISPATCH) {
                publishTaskStatusChangeEvent(task)
            }
        }

        val dispatchTaskRequestDTO = taskAssembler.toTaskDispatchRequestDTO(task)
        val result = workerRemoteService.dispatch(targetWorker, dispatchTaskRequestDTO)
        if (result.success && result.data == true) {
            log.info("dispatch task [{}] to worker [{}] successfully", task.id!!.value, targetWorker)
        } else {
            val exception = result.cause
            log.error(
                "dispatch task [{}] to {} failed: {}",
                task.id!!.value, targetWorker, exception?.message, exception
            )
            if (task.canReattempt) {
                task.resetStatusForReattempt()
            } else {
                task.markFailed(message = exception?.stackTraceToString()?.take(2000))
            }
            transactionTemplate.executeWithoutResult {
                taskRepository.save(task)
                publishTaskStatusChangeEvent(task)
            }
        }
    }

    private fun selectWorker(
        task: Task,
        jobInstance: JobInstance,
        candidateWorkers: Set<WorkerRegistry>
    ): String {
        val specifiedWorkerAddress = jobInstance.workerAddress
        if (specifiedWorkerAddress.orEmpty().isNotBlank()) {
            // 使用用户指定的运行机器
            return specifiedWorkerAddress!!
        }
        return if (task.executeMode != ExecuteModeEnum.BROADCAST) {
            if (task.attemptCnt!! > 0 && candidateWorkers.size > 1) {
                // 如果上次任务派发失败了，本次就换个节点派发
                candidateWorkers.asSequence()
                    .filterNot { it.address == task.workerAddress }
                    .maxBy { it.healthScore }
                    .address
            } else {
                candidateWorkers.maxBy { it.healthScore }.address
            }
        } else {
            // 广播任务不能换节点
            task.workerAddress.orEmpty()
        }
    }

    fun publishTaskStatusChangeEvent(task: Task) {
        applicationEventPublisher.publishEvent(
            TaskStatusChangeEvent.create(
                taskId = task.id!!,
                jobInstanceId = task.jobInstanceId!!,
                executeMode = task.executeMode!!,
            )
        )
    }
}