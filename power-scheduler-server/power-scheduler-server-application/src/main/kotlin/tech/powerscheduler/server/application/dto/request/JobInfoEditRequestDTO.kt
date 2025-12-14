package tech.powerscheduler.server.application.dto.request


import jakarta.validation.constraints.*
import tech.powerscheduler.common.enums.*

/**
 * 任务编辑请求参数
 *
 * @author grayrat
 * @since 2025/4/16
 */
class JobInfoEditRequestDTO {
    /**
     * 任务名称
     */
    @NotBlank
    var jobName: String? = null

    /**
     * 任务描述
     */
    var jobDesc: String? = null

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
     * 任务类型
     */
    @NotNull
    var jobType: JobTypeEnum? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 执行模式
     */
    @NotNull
    var executeMode: ExecuteModeEnum? = ExecuteModeEnum.SINGLE

    /**
     * 执行参数
     */
    var executeParams: String? = null

    /**
     * 最大实例并发数
     */
    @Positive
    var maxConcurrentNum: Int = 1

    /**
     * 脚本类型
     */
    var scriptType: ScriptTypeEnum? = null

    /**
     * 脚本源代码
     */
    var scriptCode: String? = null

    /**
     * 优先级
     */
    @PositiveOrZero
    var priority: Int = 1

    /**
     * 最大重试次数
     */
    @PositiveOrZero
    var maxAttemptCnt: Int = 0

    /**
     * 重试间隔(s)
     */
    @PositiveOrZero
    var attemptInterval: Int? = 15

    /**
     * 子任务最大重试次数
     */
    var taskMaxAttemptCnt: Int? = 0

    /**
     * 子任务重试间隔(s)
     */
    var taskAttemptInterval: Int? = 15

    /**
     * 保留策略
     */
    @NotNull
    var retentionPolicy: RetentionPolicyEnum? = RetentionPolicyEnum.RECENT_COUNT

    /**
     * 保留值
     */
    @NotNull
    @Min(1)
    var retentionValue: Int? = 300
}