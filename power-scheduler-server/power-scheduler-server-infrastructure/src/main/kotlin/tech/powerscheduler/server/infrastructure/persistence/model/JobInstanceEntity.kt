package tech.powerscheduler.server.infrastructure.persistence.model

import jakarta.persistence.*
import tech.powerscheduler.common.enums.*
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/4/16
 */
@Entity
@Table(
    name = "job_instance",
    indexes = [
        Index(name = "job_instance_idx_source_id", columnList = "source_id"),
        Index(name = "job_instance_idx_start_at", columnList = "start_at"),
        Index(name = "job_instance_idx_end_at", columnList = "end_at"),
    ]
)
class JobInstanceEntity() : BaseEntity() {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "app_group_id", nullable = false)
    var appGroupEntity: AppGroupEntity? = null

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 任务来源对象的id
     */
    @Column(name = "source_id", nullable = false, updatable = false)
    var sourceId: Long? = null

    /**
     * 任务来源
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false)
    var sourceType: JobSourceTypeEnum? = null

    /**
     * 工作流实例编码
     */
    @Column(name = "workflow_instance_code", updatable = false)
    var workflowInstanceCode: String? = null

    /**
     * 工作流节点实例编码
     */
    @Column(name = "workflow_node_instance_code", updatable = false)
    var workflowNodeInstanceCode: String? = null

    /**
     * worker地址(指定机器运行时使用)
     */
    @Column(name = "worker_address")
    var workerAddress: String? = null

    /**
     * 调度端ip
     */
    @Column(name = "scheduler_address")
    var schedulerAddress: String? = null

    /**
     * 任务名称
     */
    @Column(name = "job_name", nullable = false, updatable = false)
    var jobName: String? = null

    /**
     * 任务类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, updatable = false)
    var jobType: JobTypeEnum? = null

    /**
     * 任务处理器
     */
    @Column(name = "processor", nullable = false, updatable = false)
    var processor: String? = null

    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false)
    var jobStatus: JobStatusEnum? = null

    /**
     * 触发时间
     */
    @Column(name = "schedule_at", nullable = false)
    var scheduleAt: LocalDateTime? = null

    /**
     * 开始时间
     */
    @Column(name = "start_at", insertable = false)
    var startAt: LocalDateTime? = null

    /**
     * 结束时间
     */
    @Column(name = "end_at", insertable = false)
    var endAt: LocalDateTime? = null

    /**
     * 任务参数
     */
    @Column(name = "execute_params", updatable = false)
    var executeParams: String? = null

    /**
     * 执行模式
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "execute_mode", nullable = false, updatable = false)
    var executeMode: ExecuteModeEnum? = null

    /**
     * 调度类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, updatable = false)
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 任务信息
     */
    @Column(name = "message", insertable = false, length = 6000)
    var message: String? = null

    /**
     * 数据时间
     */
    @Column(name = "data_time", updatable = false)
    var dataTime: LocalDateTime? = null

    /**
     * 脚本类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "script_type", updatable = false)
    var scriptType: ScriptTypeEnum? = null

    /**
     * 脚本源代码
     */
    @Column(name = "script_code", updatable = false)
    var scriptCode: String? = null

    /**
     * 当前重试次数
     */
    @Column(name = "attempt_cnt", nullable = false)
    var attemptCnt: Int? = null

    /**
     * 最大重试次数
     */
    @Column(name = "max_attempt_cnt", nullable = false, updatable = false)
    var maxAttemptCnt: Int? = null

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
    @Column(name = "priority", updatable = false)
    var priority: Int? = null
}