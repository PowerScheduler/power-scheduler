package tech.powerscheduler.server.infrastructure.persistence.model

import jakarta.persistence.*

/**
 * 工作流分组
 *
 * @author grayrat
 * @since 2025/12/7
 */
@Entity
@Table(name = "workflow_group")
class WorkflowGroupEntity : BaseEntity() {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "namespace_id", nullable = false)
    var namespaceEntity: NamespaceEntity? = null

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 工作流分组编码
     */
    @Column(name = "code", nullable = false, updatable = false)
    var code: String? = null

    /**
     * 工作流分组名称
     */
    @Column(name = "name", nullable = false)
    var name: String? = null
}