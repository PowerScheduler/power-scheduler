package tech.powerscheduler.server.application.dto.response

import java.time.LocalDateTime

/**
 * 任务详情查询响应结果
 *
 * @author grayrat
 * @since 2025/5/18
 */
class JobInfoDetailResponseDTO {
    /**
     * 主键
     */
    var id: Long? = null

    /**
     * 应用编码
     */
    var appCode: String? = null

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
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 执行模式
     */
    var executeMode: EnumDTO? = null

    /**
     * 执行参数
     */
    var executeParams: String? = null

    /**
     * 下次触发时间
     */
    var nextScheduleAt: LocalDateTime? = null

    /**
     * 任务启用状态
     */
    var enabled: Boolean? = null

    /**
     * 任务并发数
     */
    var maxConcurrentNum: Int? = null

    /**
     * 脚本类型
     */
    var scriptType: EnumDTO? = null

    /**
     * 脚本源代码
     */
    var scriptCode: String? = null

    /**
     * 最大重试次数
     */
    var maxAttemptCnt: Int? = null

    /**
     * 重试间隔(s)
     */
    var attemptInterval: Int? = null

    /**
     * 子任务最大重试次数
     */
    var taskMaxAttemptCnt: Int? = null

    /**
     * 子任务重试间隔(s)
     */
    var taskAttemptInterval: Int? = null

    /**
     * 优先级
     */
    var priority: Int? = null

    /**
     * 保留策略
     */
    var retentionPolicy: EnumDTO? = null

    /**
     * 保留值
     */
    var retentionValue: Int? = null

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