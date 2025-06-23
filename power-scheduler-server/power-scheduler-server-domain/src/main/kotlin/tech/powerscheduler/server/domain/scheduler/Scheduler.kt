package tech.powerscheduler.server.domain.scheduler

import java.time.LocalDateTime

/**
 * @author grayrat
 * @since 2025/7/11
 */
class Scheduler {
    /**
     * 主键
     */
    var id: SchedulerId? = null

    /**
     * 网络地址
     */
    var address: String? = null

    /**
     * 是否在线
     */
    var online: Boolean? = null

    /**
     * 最后心跳时间
     */
    var lastHeartbeatAt: LocalDateTime? = null

    val expired
        get() = online!!.not() || lastHeartbeatAt!!.plusSeconds(EXPIRE_TIME) < LocalDateTime.now()

    companion object {
        const val EXPIRE_TIME = 10L
    }
}