package tech.powerscheduler.server.infrastructure.persistence.model

import jakarta.persistence.*
import tech.powerscheduler.common.enums.*
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Entity
@Table(name = "job_info")
class JobInfoEntity : BaseEntity() {

    /**
     * 应用分组
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "app_group_id", nullable = false)
    var appGroupEntity: AppGroupEntity? = null

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 任务名称
     */
    @Column(name = "job_name", nullable = false)
    var jobName: String? = null

    /**
     * 任务描述
     */
    @Column(name = "job_desc", nullable = true)
    var jobDesc: String? = null

    /**
     * 任务类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    var jobType: JobTypeEnum? = null

    /**
     * 调度类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 调度配置
     */
    @Column(name = "schedule_config", nullable = false)
    var scheduleConfig: String? = null

    /**
     * 任务处理器
     */
    @Column(name = "processor", nullable = false)
    var processor: String? = null

    /**
     * 执行模式
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "execute_mode", nullable = false)
    var executeMode: ExecuteModeEnum? = null

    /**
     * 执行参数
     */
    @Column(name = "execute_params")
    var executeParams: String? = null

    /**
     * 下次执行时间
     */
    @Column(name = "next_schedule_at", insertable = false)
    var nextScheduleAt: LocalDateTime? = null

    /**
     * 任务启用状态
     */
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean? = null

    /**
     * 任务并发数
     */
    @Column(name = "max_concurrent_num", nullable = false)
    var maxConcurrentNum: Int? = null

    /**
     * 脚本类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "script_type")
    var scriptType: ScriptTypeEnum? = null

    /**
     * 脚本源代码
     */
    @Column(name = "script_code")
    var scriptCode: String? = null

    /**
     * 最大重试次数
     */
    @Column(name = "max_attempt_cnt")
    var maxAttemptCnt: Int? = null

    /**
     * 重试间隔(s)
     */
    @Column(name = "attempt_interval")
    var attemptInterval: Int? = null

    /**
     * 子任务最大重试次数
     */
    @Column(name = "task_max_attempt_cnt")
    var taskMaxAttemptCnt: Int? = null

    /**
     * 子任务重试间隔(s)
     */
    @Column(name = "task_attempt_interval")
    var taskAttemptInterval: Int? = null

    /**
     * 优先级
     */
    @Column(name = "priority")
    var priority: Int? = null

    /**
     * 上次完成时间
     */
    @Column(name = "last_completed_at", insertable = false)
    var lastCompletedAt: LocalDateTime? = null

    /**
     * 调度器地址
     */
    @Column(name = "scheduler_address")
    var schedulerAddress: String? = null

    /**
     * 保留策略
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "retention_policy")
    var retentionPolicy: RetentionPolicyEnum? = null

    /**
     * 保留值
     */
    @Column(name = "retention_value")
    var retentionValue: Int? = null
}