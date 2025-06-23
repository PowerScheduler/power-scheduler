package tech.powerscheduler.server.application.assembler

import org.springframework.stereotype.Component
import tech.powerscheduler.common.enums.JobSourceTypeEnum
import tech.powerscheduler.server.application.dto.request.JobInstanceQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.JobInstanceDetailResponseDTO
import tech.powerscheduler.server.application.dto.response.JobInstanceQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.job.JobInstance
import tech.powerscheduler.server.domain.job.JobInstanceQuery

/**
 * @author grayrat
 * @since 2025/4/27
 */
@Component
class JobInstanceAssembler {

    fun toDomainQuery(param: JobInstanceQueryRequestDTO): JobInstanceQuery {
        return JobInstanceQuery().apply {
            this.pageNo = param.pageNo
            this.pageSize = param.pageSize
            this.namespaceCode = param.namespaceCode
            this.appCode = param.appCode
            this.sourceId = param.jobId
            this.sourceType = JobSourceTypeEnum.JOB
            this.jobInstanceId = param.jobInstanceId
            this.jobName = param.jobName
            this.jobStatus = param.jobStatus
            this.startAtRange = param.startAtRange
            this.endAtRange = param.endAtRange
        }
    }

    fun toJobInstanceQueryResponseDTO(jobInstance: JobInstance): JobInstanceQueryResponseDTO {
        return JobInstanceQueryResponseDTO().apply {
            this.id = jobInstance.id!!.value
            this.jobId = jobInstance.sourceId?.value
            this.appCode = jobInstance.appGroup?.code
            this.appName = jobInstance.appGroup?.name
            this.jobName = jobInstance.jobName
            this.jobType = jobInstance.jobType.toDTO()
            this.processor = jobInstance.processor
            this.jobStatus = jobInstance.jobStatus.toDTO()
            this.scheduleAt = jobInstance.scheduleAt
            this.startAt = jobInstance.startAt
            this.endAt = jobInstance.endAt
            this.executeParams = jobInstance.executeParams
            this.executeMode = jobInstance.executeMode.toDTO()
            this.scheduleType = jobInstance.scheduleType.toDTO()
            this.message = jobInstance.message
            this.dataTime = jobInstance.dataTime
            this.scriptType = jobInstance.scriptType.toDTO()
            this.scriptCode = jobInstance.scriptCode
            this.attemptCnt = jobInstance.attemptCnt
            this.priority = jobInstance.priority
            this.createdBy = jobInstance.createdBy
            this.createdAt = jobInstance.createdAt
            this.updatedBy = jobInstance.updatedBy
            this.updatedAt = jobInstance.updatedAt
        }
    }

    fun toJobInstanceQueryDetailDTO(jobInstance: JobInstance): JobInstanceDetailResponseDTO {
        return JobInstanceDetailResponseDTO().apply {
            this.id = jobInstance.id!!.value
            this.jobId = jobInstance.sourceId?.value
            this.appCode = jobInstance.appGroup?.code
            this.appName = jobInstance.appGroup?.name
            this.schedulerAddress = jobInstance.schedulerAddress
            this.workerAddress = jobInstance.workerAddress
            this.jobName = jobInstance.jobName
            this.jobType = jobInstance.jobType.toDTO()
            this.processor = jobInstance.processor
            this.jobStatus = jobInstance.jobStatus.toDTO()
            this.scheduleAt = jobInstance.scheduleAt
            this.startAt = jobInstance.startAt
            this.endAt = jobInstance.endAt
            this.executeParams = jobInstance.executeParams
            this.executeMode = jobInstance.executeMode.toDTO()
            this.scheduleType = jobInstance.scheduleType.toDTO()
            this.message = jobInstance.message
            this.dataTime = jobInstance.dataTime
            this.scriptType = jobInstance.scriptType.toDTO()
            this.scriptCode = jobInstance.scriptCode
            this.attemptCnt = jobInstance.attemptCnt
            this.priority = jobInstance.priority
        }
    }

}