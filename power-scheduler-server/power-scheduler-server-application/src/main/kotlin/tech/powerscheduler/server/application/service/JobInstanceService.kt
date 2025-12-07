package tech.powerscheduler.server.application.service

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.enums.JobSourceTypeEnum.JOB
import tech.powerscheduler.common.enums.JobSourceTypeEnum.WORKFLOW
import tech.powerscheduler.common.enums.JobStatusEnum
import tech.powerscheduler.common.enums.JobStatusEnum.*
import tech.powerscheduler.common.enums.ScheduleTypeEnum
import tech.powerscheduler.common.enums.TaskTypeEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.JobInstanceAssembler
import tech.powerscheduler.server.application.assembler.TaskAssembler
import tech.powerscheduler.server.application.dto.request.JobInstanceQueryRequestDTO
import tech.powerscheduler.server.application.dto.request.JobProgressQueryRequestDTO
import tech.powerscheduler.server.application.dto.request.JobRunRequestDTO
import tech.powerscheduler.server.application.dto.response.JobInstanceDetailResponseDTO
import tech.powerscheduler.server.application.dto.response.JobInstanceQueryResponseDTO
import tech.powerscheduler.server.application.dto.response.JobProgressQueryResponseDTO
import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.domainevent.AggregateTypeEnum
import tech.powerscheduler.server.domain.domainevent.DomainEvent
import tech.powerscheduler.server.domain.domainevent.DomainEventRepository
import tech.powerscheduler.server.domain.domainevent.DomainEventTypeEnum
import tech.powerscheduler.server.domain.job.*
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstanceStatusChangeEvent
import java.time.LocalDateTime

/**
 * 任务实例相关服务
 *
 * @author grayrat
 * @since 2025/4/16
 */
@Service
class JobInstanceService(
    private val taskAssembler: TaskAssembler,
    private val taskRepository: TaskRepository,
    private val jobInfoRepository: JobInfoRepository,
    private val jobInstanceRepository: JobInstanceRepository,
    private val jobInstanceAssembler: JobInstanceAssembler,
    private val domainEventRepository: DomainEventRepository,
    private val workflowInstanceRepository: WorkflowInstanceRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun list(param: JobInstanceQueryRequestDTO): PageDTO<JobInstanceQueryResponseDTO> {
        val query = jobInstanceAssembler.toDomainQuery(param)
        val page = jobInstanceRepository.pageQuery(query)
        return page.toDTO().map { jobInstanceAssembler.toJobInstanceQueryResponseDTO(it) }
    }

    fun detail(jobInstanceId: Long): JobInstanceDetailResponseDTO? {
        val jobInstance = jobInstanceRepository.findById(JobInstanceId(jobInstanceId))
        return jobInstance?.let { jobInstanceAssembler.toJobInstanceQueryDetailDTO(it) }
    }

    fun getErrorMessage(jobInstanceId: Long): String {
        val jobInstance = jobInstanceRepository.findById(JobInstanceId(jobInstanceId))
            ?: throw BizException(message = "重跑任务失败: 任务实例不存在")
        return jobInstance.takeIf { it.jobStatus == FAILED }?.result.orEmpty()
    }

    fun terminate(jobInstanceId: Long) {
        val jobInstance = jobInstanceRepository.findById(JobInstanceId(jobInstanceId))
            ?: throw BizException(message = "终止任务失败: 任务实例不存在")
        when (jobInstance.jobStatus!!) {
            WAITING_SCHEDULE, WAITING_DISPATCH, PENDING, PROCESSING -> {
                jobInstance.terminate()
                jobInstanceRepository.save(jobInstance)
                val terminatedEvent = JobInstanceTerminatedEvent(
                    jobInstanceId = JobInstanceId(jobInstanceId),
                )
                applicationEventPublisher.publishEvent(terminatedEvent)
            }

            FAILED, SUCCESS -> throw BizException("终止任务失败: 任务已经完成")
        }
    }

    fun run(param: JobRunRequestDTO): Long {
        val jobInfo = jobInfoRepository.findById(JobId(param.jobId!!))
            ?: throw BizException(message = "运行任务失败: 任务不存在")
        val jobInstance = jobInfo.createInstance().apply {
            this.dataTime = param.dataTime
            this.workerAddress = param.workerAddress
            this.executeParams = param.executeParams
            this.maxAttemptCnt = 0
            this.scheduleAt = LocalDateTime.now()
        }
        val jobInstanceId = jobInstanceRepository.save(jobInstance)
        return jobInstanceId.value
    }

    fun retry(jobInstanceId: Long): Long {
        val jobInstance = jobInstanceRepository.findById(JobInstanceId(jobInstanceId))
            ?: throw BizException(message = "重跑任务失败: 任务实例不存在")
        val jobInstanceToRetry = jobInstance.cloneForRetry()
        val jobInstanceId = jobInstanceRepository.save(jobInstanceToRetry)
        return jobInstanceId.value
    }

    fun queryProgress(param: JobProgressQueryRequestDTO): PageDTO<JobProgressQueryResponseDTO> {
        val jobInstanceId = JobInstanceId(param.jobInstanceId!!)
        val jobInstance = jobInstanceRepository.findById(jobInstanceId) ?: return PageDTO.empty()
        val batch = jobInstance.batch!!
        val pageQuery = PageQuery().also {
            it.pageNo = param.pageNo
            it.pageSize = param.pageSize
        }
        val page = taskRepository.findAllByJobInstanceIdAndBatchAndTaskType(
            jobInstanceId = jobInstanceId,
            batch = batch,
            taskTypes = TaskTypeEnum.entries,
            pageQuery = pageQuery
        )
        return page.toDTO().map { taskAssembler.toJobProgressQueryResponseDTO(it) }
    }

    @Transactional
    fun updateJobInstanceProgress(jobInstanceId: JobInstanceId) {
        val jobInstance = jobInstanceRepository.lockById(jobInstanceId)
        if (jobInstance == null) {
            log.warn("更新任务状态失败: 任务实例[${jobInstanceId.value}]不存在")
            return
        }
        if (jobInstance.jobStatus in JobStatusEnum.COMPLETED_STATUSES) {
            log.info("updateProgress cancel, jobInstance [{}] is [{}]", jobInstanceId.value, jobInstance.jobStatus)
            return
        }
        val tasks = taskRepository.findAllByJobInstanceIdAndBatchAndTaskType(
            jobInstanceId = jobInstanceId,
            batch = jobInstance.batch!!
        )
        val oldStatus = jobInstance.jobStatus
        jobInstance.updateProgress(tasks)
        // 如果计算出的任务状态与当前一样, 则不需要更新状态
        if (oldStatus == jobInstance.jobStatus) {
            return
        }
        jobInstanceRepository.save(jobInstance)
        when (jobInstance.sourceType!!) {
            JOB -> updateJobInfo(jobInstance)
            WORKFLOW -> updateWorkflowInstance(jobInstance)
        }
        log.info("jobInstance updateProgress successfully: id={}, status={}", jobInstanceId.value, jobInstance.jobStatus)
    }

    private fun updateWorkflowInstance(jobInstance: JobInstance) {
        val workflowInstanceCode = jobInstance.workflowInstanceCode!!
        val workflowNodeInstanceCode = jobInstance.workflowNodeInstanceCode
        val workflowInstance = workflowInstanceRepository.lockByCode(workflowInstanceCode)
        if (workflowInstance == null) {
            return
        }
        val workflowNodeInstance = workflowInstance.workflowNodeInstances.find {
            it.nodeInstanceCode == workflowNodeInstanceCode
        }!!
        val newStatus = WorkflowStatusEnum.from(jobInstance.jobStatus!!)
        if (workflowNodeInstance.status == newStatus) {
            return
        }
        workflowNodeInstance.apply {
            this.startAt = jobInstance.startAt
            this.endAt = jobInstance.endAt
            this.status = newStatus
            this.workerAddress = jobInstance.workerAddress
        }
        workflowInstance.apply {
            this.graphData!!.mapNotNull { it.data }
                .find { it.workflowNodeInstanceCode == workflowNodeInstanceCode }
                ?.also { it.status = workflowNodeInstance.status }
        }
        workflowInstanceRepository.save(workflowInstance)
        val workflowInstanceId = workflowInstance.id!!
        val domainEvent = DomainEvent.create(
            aggregateId = workflowInstanceId.value.toString(),
            aggregateType = AggregateTypeEnum.WORKFLOW_INSTANCE,
            eventType = DomainEventTypeEnum.WORKFLOW_NODE_INSTANCE_STATUS_CHANGED,
            body = JSON.writeValueAsString(WorkflowNodeInstanceStatusChangeEvent.create(workflowInstanceId))
        )
        domainEventRepository.save(domainEvent)
        log.info(
            "workflowNodeInstance update successfully: id={}, status={}",
            workflowNodeInstance.id!!.value, workflowNodeInstance.status
        )
    }

    private fun updateJobInfo(jobInstance: JobInstance) {
        val jobInfo = jobInfoRepository.lockById(jobInstance.sourceId!!.toJobId())
        if (jobInfo == null) {
            return
        }
        jobInfo.lastCompletedAt = jobInstance.endAt
        if (jobInfo.scheduleType == ScheduleTypeEnum.ONE_TIME) {
            jobInfo.enabled = false
        }
        if (jobInfo.scheduleType == ScheduleTypeEnum.FIX_DELAY) {
            jobInfo.updateNextScheduleTime()
        }
        jobInfoRepository.save(jobInfo)
    }
}