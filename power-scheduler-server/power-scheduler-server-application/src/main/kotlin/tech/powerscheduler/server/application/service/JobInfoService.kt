package tech.powerscheduler.server.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.powerscheduler.common.dto.response.PageDTO
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.application.assembler.JobInfoAssembler
import tech.powerscheduler.server.application.dto.request.JobInfoAddRequestDTO
import tech.powerscheduler.server.application.dto.request.JobInfoEditRequestDTO
import tech.powerscheduler.server.application.dto.request.JobInfoQueryRequestDTO
import tech.powerscheduler.server.application.dto.request.JobSwitchRequestDTO
import tech.powerscheduler.server.application.dto.response.JobInfoDetailResponseDTO
import tech.powerscheduler.server.application.dto.response.JobInfoQueryResponseDTO
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.appgroup.AppGroupRepository
import tech.powerscheduler.server.domain.job.JobId
import tech.powerscheduler.server.domain.job.JobInfoRepository
import tech.powerscheduler.server.domain.namespace.NamespaceRepository

/**
 * 任务相关服务
 *
 * @author grayrat
 * @since 2025/4/16
 */
@Service
class JobInfoService(
    private val namespaceRepository: NamespaceRepository,
    private val appGroupRepository: AppGroupRepository,
    private val jobInfoRepository: JobInfoRepository,
    private val jobInfoAssembler: JobInfoAssembler,
) {

    fun query(param: JobInfoQueryRequestDTO): PageDTO<JobInfoQueryResponseDTO> {
        val query = jobInfoAssembler.toDomainQuery(param)
        val page = jobInfoRepository.pageQuery(query)
        return page.toDTO().map { jobInfoAssembler.toJobInfoQueryResponseDTO(it) }
    }

    fun detail(jobId: Long): JobInfoDetailResponseDTO? {
        val jobInfo = jobInfoRepository.findById(JobId(jobId))
        return jobInfo?.let { jobInfoAssembler.toJobInfoDetailResponseDTO(it) }
    }

    fun add(param: JobInfoAddRequestDTO): Long {
        val appCode = param.appCode!!
        val namespaceCode = param.namespaceCode!!
        val namespace = namespaceRepository.findByCode(namespaceCode)
            ?: throw BizException("任务新增失败: 命名空间[$namespaceCode]不存在")
        val appGroup = appGroupRepository.findByCode(namespace, appCode)
            ?: throw BizException(message = "任务新增失败: 应用分组不存在")
        val jobInfo = jobInfoAssembler.toDomainModel4AddRequest(param, appGroup).apply {
            validate()
        }
        val jobId = jobInfoRepository.save(jobInfo)
        return jobId.value
    }

    @Transactional
    fun edit(param: JobInfoEditRequestDTO) {
        val jobId = JobId(param.jobId!!)
        val jobInfo = jobInfoRepository.lockById(jobId)
            ?: throw BizException(message = "任务保存失败: 任务不存在")
        val jobInfoToSave = jobInfoAssembler.toDomainModel4EditRequest(jobInfo, param).apply {
            validate()
        }
        jobInfoRepository.save(jobInfoToSave)
    }

    @Transactional
    fun switch(param: JobSwitchRequestDTO) {
        val jobId = JobId(param.jobId!!)
        val jobInfo = jobInfoRepository.lockById(jobId)
            ?: throw BizException("任务保存失败: 任务不存在")
        if (jobInfo.enabled == param.enabled) {
            return
        }
        jobInfo.enabled = param.enabled
        if (jobInfo.enabled == true) {
            jobInfo.initNextScheduleTime()
        } else {
            jobInfo.nextScheduleAt = null
        }
        jobInfoRepository.save(jobInfo)
    }

    fun remove(jobId: Long) {
        val jobIdToRemove = JobId(jobId)
        val jobInfo = jobInfoRepository.findById(jobIdToRemove)
        if (jobInfo == null) {
            return
        }
        jobInfoRepository.deleteById(jobIdToRemove)
    }

}