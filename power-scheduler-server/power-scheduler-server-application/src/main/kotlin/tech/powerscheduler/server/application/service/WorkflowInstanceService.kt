package tech.powerscheduler.server.application.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.enums.JobStatusEnum.*
import tech.powerscheduler.common.enums.TaskTypeEnum
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.TaskAssembler
import tech.powerscheduler.server.application.assembler.WorkflowInstanceAssembler
import tech.powerscheduler.server.application.dto.request.WorkflowInstanceQueryRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowNodeInstanceProgressQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.JobProgressQueryResponseDTO
import tech.powerscheduler.server.application.dto.response.WorkflowInstanceDetailResponseDTO
import tech.powerscheduler.server.application.dto.response.WorkflowInstanceQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.common.PageQuery
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.job.JobInstanceTerminatedEvent
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.workflow.*
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/9
 */
@Service
class WorkflowInstanceService(
    private val taskRepository: TaskRepository,
    private val jobInstanceRepository: JobInstanceRepository,
    private val workflowRepository: WorkflowRepository,
    private val workflowInstanceRepository: WorkflowInstanceRepository,
    private val workflowNodeInstanceRepository: WorkflowNodeInstanceRepository,
    private val taskAssembler: TaskAssembler,
    private val workflowInstanceAssembler: WorkflowInstanceAssembler,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    fun list(param: WorkflowInstanceQueryRequestDTO): PageDTO<WorkflowInstanceQueryResponseDTO> {
        val query = workflowInstanceAssembler.toDomainQuery(param)
        val page = workflowInstanceRepository.pageQuery(query)
        return page.toDTO().map { workflowInstanceAssembler.toWorkflowInstanceQueryResponseDTO(it) }
    }

    fun get(workflowInstanceId: Long): WorkflowInstanceDetailResponseDTO? {
        val workflowInstance = workflowInstanceRepository.findById(WorkflowInstanceId(workflowInstanceId))
        return workflowInstance?.let { workflowInstanceAssembler.toWorkflowInstanceDetailResponseDTO(it) }
    }

    fun queryProgress(
        workflowInstanceId: Long,
        param: WorkflowNodeInstanceProgressQueryRequestDTO
    ): PageDTO<JobProgressQueryResponseDTO> {
        val workflowNodeInstanceId = WorkflowNodeInstanceId(workflowInstanceId)
        val workflowNodeInstance = workflowNodeInstanceRepository.findById(workflowNodeInstanceId)
            ?: throw BizException("工作流节点不存在")
        val workflowNodeInstanceCode = workflowNodeInstance.nodeInstanceCode.orEmpty()
        val jobInstance = jobInstanceRepository.findByWorkflowNodeInstanceCode(workflowNodeInstanceCode)
            ?: return PageDTO.empty()
        val batch = jobInstance.batch!!
        val page = taskRepository.findAllByJobInstanceIdAndBatchAndTaskType(
            jobInstanceId = jobInstance.id!!,
            batch = batch,
            taskTypes = TaskTypeEnum.entries,
            pageQuery = PageQuery().also {
                it.pageNo = param.pageNo
                it.pageSize = param.pageSize
            }
        )
        return page.toDTO().map { taskAssembler.toJobProgressQueryResponseDTO(it) }
    }

    @Transactional
    fun terminate(workflowInstanceId: Long) {
        val workflowInstance = workflowInstanceRepository.findById(WorkflowInstanceId(workflowInstanceId))
            ?: throw BizException("WorkflowInstance not found")
        val workflowNodeInstances = workflowInstance.workflowNodeInstances
        val nodeInstanceCodes = workflowNodeInstances.mapNotNull { it.nodeInstanceCode }
        val jobInstances = jobInstanceRepository.findAllByWorkflowNodeInstanceCodes(nodeInstanceCodes)

        workflowInstance.terminate()
        val terminatedEvents = jobInstances.map { jobInstance ->
            when (jobInstance.jobStatus!!) {
                WAITING_SCHEDULE, WAITING_DISPATCH, PENDING, PROCESSING -> jobInstance.terminate()
                FAILED, SUCCESS -> {}
            }
            JobInstanceTerminatedEvent(jobInstanceId = jobInstance.id!!)
        }

        workflowInstanceRepository.save(workflowInstance)
        jobInstanceRepository.saveAll(jobInstances)
        terminatedEvents.forEach { applicationEventPublisher.publishEvent(it) }
    }

    @Transactional
    fun retry(workflowInstanceId: Long): Long {
        val oldWorkflowInstance = workflowInstanceRepository.findById(WorkflowInstanceId(workflowInstanceId))
            ?: throw BizException("WorkflowInstance [$workflowInstanceId] not found")
        val workflow = workflowRepository.findById(oldWorkflowInstance.workflowId!!)
            ?: throw BizException("Workflow [${oldWorkflowInstance.workflowId}] not found")
        val workflowInstance = workflow.createInstance(
            scheduleAt = LocalDateTime.now(),
            dataTime = oldWorkflowInstance.dataTime ?: LocalDateTime.now(),
        )
        val workflowNodeInstances = workflowInstance.workflowNodeInstances.onEach {
            it.dataTime = workflowInstance.dataTime
            it.maxAttemptCnt = 0
            it.taskMaxAttemptCnt = 0
        }
        val jobInstances = workflowNodeInstances.filter { it.parents.isEmpty() }.map { it.createJobInstance() }
        jobInstanceRepository.saveAll(jobInstances)
        val newWorkflowInstanceId = workflowInstanceRepository.save(workflowInstance)
        return newWorkflowInstanceId.value
    }
}