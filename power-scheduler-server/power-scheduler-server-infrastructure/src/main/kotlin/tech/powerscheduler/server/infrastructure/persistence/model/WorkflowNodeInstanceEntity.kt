package tech.powerscheduler.server.infrastructure.persistence.model

import jakarta.persistence.*
import tech.powerscheduler.common.enums.ExecuteModeEnum
import tech.powerscheduler.common.enums.JobTypeEnum
import tech.powerscheduler.common.enums.ScriptTypeEnum
import tech.powerscheduler.common.enums.WorkflowStatusEnum
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/22
 */
@Entity
@Table(name = "workflow_node_instance")
class WorkflowNodeInstanceEntity : BaseEntity() {
    /**
     * 子节点
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "workflow_node_instance_children",
        joinColumns = [JoinColumn(name = "workflow_node_instance_id")],
        inverseJoinColumns = [JoinColumn(name = "workflow_node_instance_child_id")]
    )
    var children: Set<WorkflowNodeInstanceEntity> = emptySet()

    /**
     * 父节点
     */
    @ManyToMany(mappedBy = "children", fetch = FetchType.EAGER)
    var parents: Set<WorkflowNodeInstanceEntity> = emptySet()

    /**
     * 工作流实例
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    var workflowInstanceEntity: WorkflowInstanceEntity? = null

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 流节点编码
     */
    @Column(name = "node_code", nullable = false, updatable = false)
    var nodeCode: String? = null

    /**
     * 节点实例编号
     */
    @Column(name = "node_instance_code", nullable = false, updatable = false)
    var nodeInstanceCode: String? = null

    /**
     * 任务名称
     */
    @Column(name = "job_name", nullable = false)
    var name: String? = null

    /**
     * 任务类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    var jobType: JobTypeEnum? = null

    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: WorkflowStatusEnum? = null

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
     * 任务参数
     */
    @Column(name = "execute_params")
    var executeParams: String? = null

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
     * 数据时间
     */
    @Column(name = "data_time", nullable = false)
    var dataTime: LocalDateTime? = null

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
     * Worker地址（ip:host）
     */
    @Column(name = "worker_address")
    var workerAddress: String? = null

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

//    /**
//     * 触发时间
//     */
//    @Column(name = "schedule_at")
//    var scheduleAt: LocalDateTime? = null

    /**
     * 调度端ip
     */
    @Column(name = "scheduler_address")
    var schedulerAddress: String? = null
}