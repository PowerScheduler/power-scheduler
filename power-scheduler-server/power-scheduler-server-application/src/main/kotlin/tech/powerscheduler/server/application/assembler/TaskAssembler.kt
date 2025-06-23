package tech.powerscheduler.server.application.assembler

import org.springframework.stereotype.Component
import tech.powerscheduler.common.dto.request.JobDispatchRequestDTO
import tech.powerscheduler.common.dto.response.FetchTaskResultResponseDTO
import tech.powerscheduler.common.enums.TaskTypeEnum
import tech.powerscheduler.server.application.dto.response.JobProgressQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.task.Task

/**
 * @author grayrat
 * @since 2025/6/7
 */
@Component
class TaskAssembler {

    fun toTaskDispatchRequestDTO(task: Task): JobDispatchRequestDTO {
        return JobDispatchRequestDTO().also {
            it.jobInstanceId = task.jobInstanceId!!.value
            it.taskId = task.id!!.value
            it.taskName = task.taskName
            it.parentTaskId = task.parentId?.value
            it.appCode = task.appGroup?.code
            it.jobType = task.jobType
            it.processor = task.processor
            it.jobStatus = task.taskStatus
            it.executeParams = task.executeParams
            it.scheduleAt = task.scheduleAt
            it.executeMode = task.executeMode
            it.dataTime = task.dataTime
            it.scriptType = task.scriptType
            it.scriptCode = task.scriptCode
            it.attemptCnt = task.attemptCnt
            it.priority = task.priority ?: 0
            it.taskType = task.taskType
            if (it.taskType == TaskTypeEnum.SUB) {
                it.taskBody = task.taskBody
            }
        }
    }

    fun toJobProgressQueryResponseDTO(task: Task): JobProgressQueryResponseDTO {
        return JobProgressQueryResponseDTO().also {
            it.taskId = task.id!!.value
            it.jobInstanceId = task.jobInstanceId!!.value
            it.taskName = task.taskName
            it.taskStatus = task.taskStatus.toDTO()
            it.workerAddress = task.workerAddress
            it.startAt = task.startAt
            it.endAt = task.endAt
        }
    }

    fun toFetchTaskResultResponseDTO(task: Task): FetchTaskResultResponseDTO {
        return FetchTaskResultResponseDTO().also {
            it.taskId = task.id!!.value
            it.taskName = task.taskName
            it.result = task.result
        }
    }
}