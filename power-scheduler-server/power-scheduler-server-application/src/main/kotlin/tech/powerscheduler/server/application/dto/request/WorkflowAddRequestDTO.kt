package tech.powerscheduler.server.application.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import tech.powerscheduler.common.enums.RetentionPolicyEnum
import tech.powerscheduler.common.enums.ScheduleTypeEnum

/**
 * @author grayrat
 * @since 2025/6/23
 */
class WorkflowAddRequestDTO {
    /**
     * 有向无环图
     */
    @NotEmpty
    @Valid
    var nodes: List<WorkflowNodeDTO> = emptyList()

    /**
     * 命名空间编码
     */
    @NotBlank
    var namespaceCode: String? = null

    /**
     * 应用编码
     */
    @NotBlank
    var appCode: String? = null

    /**
     * 工作流名称
     */
    @NotBlank
    var name: String? = null

    /**
     * 工作流描述
     */
    var description: String? = null

    /**
     * 调度类型
     */
    @NotNull
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 调度配置
     */
    @NotBlank
    var scheduleConfig: String? = null

    /**
     * 并发数
     */
    @NotNull
    @Positive
    var maxConcurrentNum: Int? = 1

    /**
     * 保留策略
     */
    @NotNull
    var retentionPolicy: RetentionPolicyEnum? = RetentionPolicyEnum.RECENT_COUNT

    /**
     * 保留值
     */
    @NotNull
    @Positive
    var retentionValue: Int? = 100

    /**
     * 图的UI数据
     */
    @NotBlank
    var graphData: String? = null
}