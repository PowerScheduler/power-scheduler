package tech.powerscheduler.server.infrastructure.persistence.model

import jakarta.persistence.*
import tech.powerscheduler.common.enums.RetentionPolicyEnum
import tech.powerscheduler.common.enums.ScheduleTypeEnum
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/6/22
 */
@Entity
@Table(name = "workflow")
class WorkflowEntity : BaseEntity() {
    /**
     * 应用分组
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "app_group_id", nullable = false)
    var appGroupEntity: AppGroupEntity? = null

    @OneToMany(
        mappedBy = "workflowEntity",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    var workflowNodeEntities: Set<WorkflowNodeEntity> = emptySet()

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 工作流名称
     */
    @Column(name = "name", nullable = false)
    var name: String? = null

    /**
     * 工作流描述
     */
    @Column(name = "description")
    var description: String? = null

    /**
     * 有向无环图的UI数据
     */
    @Column(name = "graph_data", nullable = false, length = 40000)
    var graphData: String? = null

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
     * 下次执行时间
     */
    @Column(name = "next_schedule_at", insertable = false)
    var nextScheduleAt: LocalDateTime? = null

    /**
     * 启用状态
     */
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean? = null

    /**
     * 调度器地址
     */
    @Column(name = "scheduler_address")
    var schedulerAddress: String? = null

    /**
     * 并发数
     */
    @Column(name = "max_concurrent_num", nullable = false)
    var maxConcurrentNum: Int? = null

    /**
     * 上次完成时间
     */
    @Column(name = "last_completed_at")
    var lastCompletedAt: LocalDateTime? = null

    /**
     * 保留策略
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "retention_policy", nullable = false)
    var retentionPolicy: RetentionPolicyEnum? = null

    /**
     * 保留值
     */
    @Column(name = "retention_value", nullable = false)
    var retentionValue: Int? = null
}