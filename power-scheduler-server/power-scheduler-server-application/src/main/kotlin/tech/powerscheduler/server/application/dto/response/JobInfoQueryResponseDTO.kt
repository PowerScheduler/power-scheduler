package tech.powerscheduler.server.application.dto.response

import java.time.LocalDateTime

/**
 * 任务查询响应结果
 *
 * @author grayrat
 * @since 2025/4/16
 */
class JobInfoQueryResponseDTO {
    /**
     * 主键
     */
    var id: Long? = null

    /**
     * 命名空间编码
     */
    var namespaceCode: String? = null

    /**
     * 应用编码
     */
    var appCode: String? = null

    /**
     * 应用名称
     */
    var appName: String? = null

    /**
     * 任务名称
     */
    var jobName: String? = null

    /**
     * 任务描述
     */
    var jobDesc: String? = null

    /**
     * 任务类型
     */
    var jobType: EnumDTO? = null

    /**
     * 调度类型
     */
    var scheduleType: EnumDTO? = null

    /**
     * 调度配置
     */
    var scheduleConfig: String? = null

    /**
     * 调度配置描述
     */
    var scheduleConfigDesc: String? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 执行模式
     */
    var executeMode: EnumDTO? = null

    /**
     * 下次触发时间
     */
    var nextScheduleAt: LocalDateTime? = null

    /**
     * 任务启用状态
     */
    var enabled: Boolean? = null

    /**
     * 脚本类型
     */
    var scriptType: EnumDTO? = null

    /**
     * 创建人
     */
    var createdBy: String? = null

    /**
     * 创建时间
     */
    var createdAt: LocalDateTime? = null

    /**
     * 修改人
     */
    var updatedBy: String? = null

    /**
     * 修改时间
     */
    var updatedAt: LocalDateTime? = null
}