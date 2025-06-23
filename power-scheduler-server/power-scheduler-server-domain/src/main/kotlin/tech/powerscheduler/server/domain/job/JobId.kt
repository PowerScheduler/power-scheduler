package tech.powerscheduler.server.domain.job

/**
 * 任务ID
 *
 * @author grayrat
 * @since 2025/4/18
 */
@JvmInline
value class JobId(val value: Long){
    fun toSourceId(): SourceId {
        return SourceId(value)
    }
}