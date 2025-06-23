package tech.powerscheduler.server.infrastructure.persistence.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/11
 */
@Entity
@Table(name = "scheduler")
class SchedulerEntity : BaseEntity() {
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 网络地址
     */
    @Column(name = "address", nullable = false, updatable = false)
    var address: String? = null

    /**
     * 是否在线
     */
    @Column(name = "online", nullable = false)
    var online: Boolean? = null

    /**
     * 最后心跳时间
     */
    @Column(name = "last_heartbeat_at", nullable = false)
    var lastHeartbeatAt: LocalDateTime? = null
}