package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.WorkflowInstanceAssembler
import tech.powerscheduler.server.application.dto.request.WorkflowInstanceQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.WorkflowInstanceDetailResponseDTO
import tech.powerscheduler.server.application.dto.response.WorkflowInstanceQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.job.JobInstanceRepository
import tech.powerscheduler.server.domain.task.TaskRepository
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceId
import tech.powerscheduler.server.domain.workflow.WorkflowInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowNodeInstanceRepository
import tech.powerscheduler.server.domain.workflow.WorkflowRepository
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
    private val workflowInstanceAssembler: WorkflowInstanceAssembler,
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

    @Transactional
    fun retry(workflowInstanceId: Long) {
        val oldWorkflowInstance = workflowInstanceRepository.findById(WorkflowInstanceId(workflowInstanceId))
            ?: throw BizException("WorkflowInstance not found")
        val workflow = (workflowRepository.findById(oldWorkflowInstance.workflowId!!)
            ?: throw BizException("WorkflowInstance not found"))
        val workflowInstance = workflow.createInstance(
            scheduleAt = oldWorkflowInstance.dataTime ?: LocalDateTime.now(),
            dataTime = oldWorkflowInstance.dataTime ?: LocalDateTime.now(),
        )
        val workflowNodeInstances = workflowInstance.workflowNodeInstances.onEach {
            it.dataTime = workflowInstance.dataTime
            it.maxAttemptCnt = 0
            it.taskMaxAttemptCnt = 0
        }
        val jobInstances = workflowNodeInstances.filter { it.parents.isEmpty() }.map { it.createJobInstance() }
        workflowInstanceRepository.save(workflowInstance)
        jobInstanceRepository.saveAll(jobInstances)
    }
}