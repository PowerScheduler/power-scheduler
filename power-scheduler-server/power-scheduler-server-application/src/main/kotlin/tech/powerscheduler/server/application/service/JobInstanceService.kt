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
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.*
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    private val workflowRepository: WorkflowRepository,
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

    fun run(jobId: Long, param: JobRunRequestDTO): Long {
        val jobInfo = jobInfoRepository.findById(JobId(jobId))
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

    fun queryProgress(
        jobInstanceId: Long,
        param: JobProgressQueryRequestDTO
    ): PageDTO<JobProgressQueryResponseDTO> {
        val jobInstance = jobInstanceRepository.findById(JobInstanceId(jobInstanceId))
            ?: return PageDTO.empty()
        val batch = jobInstance.batch!!
        val pageQuery = PageQuery().also {
            it.pageNo = param.pageNo
            it.pageSize = param.pageSize
        }
        val page = taskRepository.findAllByJobInstanceIdAndBatchAndTaskType(
            jobInstanceId = JobInstanceId(jobInstanceId),
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
        val tasks = taskRepository.findAllByJobInstanceIdAndBatch(
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
        log.info(
            "jobInstance updateProgress successfully: id={}, status={}",
            jobInstanceId.value,
            jobInstance.jobStatus
        )
    }

    @Transactional
    fun updateWorkflowInstance(jobInstance: JobInstance) {
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
        workflowNodeInstance.apply {
            this.startAt = jobInstance.startAt
            this.endAt = jobInstance.endAt
            this.status = newStatus
            this.workerAddress = jobInstance.workerAddress
        }
        workflowInstance.apply {
            this.updateProgress()
            this.graphData!!.mapNotNull { it.data }
                .find { it.workflowNodeInstanceCode == workflowNodeInstanceCode }
                ?.also {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    it.status = workflowNodeInstance.status
                    it.startAt = workflowNodeInstance.startAt?.format(formatter)
                    it.endAt = workflowNodeInstance.endAt?.format(formatter)
                }
        }
        if (workflowInstance.status == WorkflowStatusEnum.RUNNING) {
            val nextWorkflowNodeInstances = workflowInstance.workflowNodeInstances
                .filter { it.status == WorkflowStatusEnum.WAITING }
                .filter { it.parents.all { parent -> parent.status == WorkflowStatusEnum.SUCCESS } }
            val jobInstances = nextWorkflowNodeInstances.map { it.createJobInstance() }
            if (jobInstances.isNotEmpty()) {
                jobInstanceRepository.saveAll(jobInstances)
                log.info("nodeInstance {} is ready to run", nextWorkflowNodeInstances.map { it.id!!.value })
            }
        }
        if (workflowInstance.status in WorkflowStatusEnum.COMPLETED_STATUSES) {
            val workflow = workflowRepository.lockById(workflowInstance.workflowId!!) ?: return
            workflow.lastCompletedAt = LocalDateTime.now()
            workflow.updateNextScheduleTime()
            workflowRepository.save(workflow)
        }
        workflowInstanceRepository.save(workflowInstance)
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