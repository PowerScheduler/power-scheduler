package tech.powerscheduler.server.application.dto.response

import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/4/16
 */
class JobInstanceQueryResponseDTO {
    /**
     * 主键
     */
    var id: Long? = null

    /**
     * 任务id
     */
    var jobId: Long? = null

    /**
     * 应用编码
     */
    var appCode: String? = null

    /**
     * 应用名称
     */
    var appName: String? = null

    /**
     * 执行器地址
     */
    var workerAddress: String? = null

    /**
     * 任务名称
     */
    var jobName: String? = null

    /**
     * 任务类型
     */
    var jobType: EnumDTO? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 任务状态
     */
    var jobStatus: EnumDTO? = null

    /**
     * 触发时间
     */
    var scheduleAt: LocalDateTime? = null

    /**
     * 开始时间
     */
    var startAt: LocalDateTime? = null

    /**
     * 结束时间
     */
    var endAt: LocalDateTime? = null

    /**
     * 任务参数
     */
    var executeParams: String? = null

    /**
     * 执行模式
     */
    var executeMode: EnumDTO? = null

    /**
     * 调度类型
     */
    var scheduleType: EnumDTO? = null

    /**
     * 任务信息
     */
    var message: String? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = null

    /**
     * 脚本类型
     */
    var scriptType: EnumDTO? = null

    /**
     * 脚本源代码
     */
    var scriptCode: String? = null

    /**
     * 当前重试次数
     */
    var attemptCnt: Int? = null

    /**
     * 优先级
     */
    var priority: Int? = null

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