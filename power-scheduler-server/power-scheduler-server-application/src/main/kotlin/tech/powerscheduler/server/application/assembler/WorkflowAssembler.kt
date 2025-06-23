package tech.powerscheduler.server.application.assembler

import org.springframework.stereotype.Component
import tech.powerscheduler.common.enums.ScheduleTypeEnum
import tech.powerscheduler.server.application.dto.request.WorkflowAddRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowEditRequestDTO
import tech.powerscheduler.server.application.dto.request.WorkflowQueryRequestDTO
import tech.powerscheduler.server.application.dto.response.WorkflowDetailResponseDTO
import tech.powerscheduler.server.application.dto.response.WorkflowQueryResponseDTO
import tech.powerscheduler.server.application.utils.JSON
import tech.powerscheduler.server.application.utils.toDTO
import tech.powerscheduler.server.domain.appgroup.AppGroup
import tech.powerscheduler.server.domain.workflow.Workflow
import tech.powerscheduler.server.domain.workflow.WorkflowGraphData
import tech.powerscheduler.server.domain.workflow.WorkflowQuery

/**
 * @author grayrat
 * @since 2025/6/23
 */
@Component
class WorkflowAssembler {

    fun toDomainQuery(param: WorkflowQueryRequestDTO): WorkflowQuery {
        return WorkflowQuery().apply {
            this.namespaceCode = param.namespaceCode
            this.appCode = param.appCode
            this.name = param.name
        }
    }

    fun toWorkflowQueryResponseDTO(workflow: Workflow): WorkflowQueryResponseDTO {
        return WorkflowQueryResponseDTO().apply {
            val appGroup = workflow.appGroup!!
            this.namespaceCode = appGroup.namespace!!.code
            this.appCode = appGroup.code
            this.appName = appGroup.name
            this.id = workflow.id!!.value
            this.name = workflow.name
            this.enabled = workflow.enabled
            this.scheduleType = workflow.scheduleType.toDTO()
            this.scheduleConfig = workflow.scheduleConfig
            this.scheduleConfigDesc = when (workflow.scheduleType!!) {
                ScheduleTypeEnum.CRON -> scheduleConfig
                ScheduleTypeEnum.FIX_RATE -> "${scheduleType!!.label} | $scheduleConfig(秒)"
                ScheduleTypeEnum.FIX_DELAY -> "${scheduleType!!.label} | $scheduleConfig(秒)"
                ScheduleTypeEnum.ONE_TIME -> scheduleConfig
            }
        }
    }

    fun toWorkflowDetailResponseDTO(workflow: Workflow): WorkflowDetailResponseDTO {
        return WorkflowDetailResponseDTO().apply {
            this.appCode = workflow.appGroup!!.code
            this.id = workflow.id!!.value
            this.name = workflow.name
            this.description = workflow.description
            this.enabled = workflow.enabled
            this.maxConcurrentNum = workflow.maxConcurrentNum
            this.retentionPolicy = workflow.retentionPolicy
            this.retentionValue = workflow.retentionValue
            this.graphData = JSON.writeValueAsString(workflow.graphData!!)
            this.scheduleType = workflow.scheduleType
            this.scheduleConfig = workflow.scheduleConfig
        }
    }

    fun toDomainModel4AddRequest(appGroup: AppGroup, param: WorkflowAddRequestDTO): Workflow {
        return Workflow().apply {
            this.appGroup = appGroup
            this.name = param.name
            this.description = param.description
            this.enabled = false
            this.maxConcurrentNum = param.maxConcurrentNum
            this.retentionPolicy = param.retentionPolicy
            this.retentionValue = param.retentionValue
            this.graphData = JSON.readValue<WorkflowGraphData>(param.graphData)
            this.scheduleType = param.scheduleType
            this.scheduleConfig = param.scheduleConfig
        }
    }

    fun toDomainModel4EditRequest(workflow: Workflow, param: WorkflowEditRequestDTO): Workflow {
        return Workflow().apply {
            this.appGroup = workflow.appGroup
            this.id = workflow.id
            this.name = param.name
            this.description = param.description
            this.enabled = false
            this.maxConcurrentNum = param.maxConcurrentNum
            this.retentionPolicy = param.retentionPolicy
            this.retentionValue = param.retentionValue
            this.graphData = JSON.readValue<WorkflowGraphData>(param.graphData)
            this.lastCompletedAt = null
            this.nextScheduleAt = null
            this.scheduleType = param.scheduleType
            this.scheduleConfig = param.scheduleConfig
        }
    }

}