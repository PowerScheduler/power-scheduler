package tech.powerscheduler.server.application.dto.response

import tech.powerscheduler.common.enums.RetentionPolicyEnum
import tech.powerscheduler.common.enums.ScheduleTypeEnum

/**
 * @author grayrat
 * @since 2025/7/5
 */
class WorkflowDetailResponseDTO {
    /**
     * 应用编码
     */
    var appCode: String? = null
    /**
     * 主键
     */
    var id: Long? = null

    /**
     * 工作流名称
     */
    var name: String? = null

    /**
     * 工作流描述
     */
    var description: String? = null

    /**
     * 启用状态
     */
    var enabled: Boolean? = null

    /**
     * 并发数
     */
    var maxConcurrentNum: Int? = null

    /**
     * 保留策略
     */
    var retentionPolicy: RetentionPolicyEnum? = null

    /**
     * 保留值
     */
    var retentionValue: Int? = null

    /**
     * 有向无环图的UI数据
     */
    var graphData: String? = null

    /**
     * 调度类型
     */
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 调度配置
     */
    var scheduleConfig: String? = null
}