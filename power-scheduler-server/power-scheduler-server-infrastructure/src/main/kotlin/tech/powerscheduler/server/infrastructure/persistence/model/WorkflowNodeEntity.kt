package tech.powerscheduler.server.infrastructure.persistence.model

import jakarta.persistence.*
import tech.powerscheduler.common.enums.ExecuteModeEnum
import tech.powerscheduler.common.enums.JobTypeEnum
import tech.powerscheduler.common.enums.ScriptTypeEnum

/**
 * 工作流节点
 *
 * @author grayrat
 * @since 2025/6/22
 */
@Entity
@Table(name = "workflow_node")
class WorkflowNodeEntity : BaseEntity() {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_id", nullable = false)
    var workflowEntity: WorkflowEntity? = null

    /**
     * 子节点集合
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "workflow_node_children",
        joinColumns = [JoinColumn(name = "workflow_node_id")],
        inverseJoinColumns = [JoinColumn(name = "workflow_node_child_id")]
    )
    var children: Set<WorkflowNodeEntity> = emptySet()

    /**
     * 父节点集合
     */
    @ManyToMany(mappedBy = "children", fetch = FetchType.EAGER)
    var parents: Set<WorkflowNodeEntity> = emptySet()

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 节点编码
     */
    @Column(name = "code", nullable = false)
    var code: String? = null

    /**
     * 任务名称
     */
    @Column(name = "name", nullable = false)
    var name: String? = null

    /**
     * 任务描述
     */
    @Column(name = "description", nullable = true)
    var description: String? = null

    /**
     * 任务类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    var jobType: JobTypeEnum? = null

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
}