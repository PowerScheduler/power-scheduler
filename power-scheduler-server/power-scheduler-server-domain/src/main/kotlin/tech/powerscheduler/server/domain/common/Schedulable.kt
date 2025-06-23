package tech.powerscheduler.server.domain.common

import tech.powerscheduler.common.enums.ScheduleTypeEnum
import tech.powerscheduler.common.enums.ScheduleTypeEnum.*
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.domain.utils.CronUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author grayrat
 * @since 2025/6/25
 */
abstract class Schedulable {

    /**
     * 下次执行时间
     */
    var nextScheduleAt: LocalDateTime? = null

    /**
     * 调度类型
     */
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 调度配置
     */
    var scheduleConfig: String? = null

    /**
     * 上次完成时间
     */
    var lastCompletedAt: LocalDateTime? = null

    fun initNextScheduleTime() {
        if (this.nextScheduleAt != null) {
            return
        }
        val now = LocalDateTime.now()
        this.nextScheduleAt = when (scheduleType!!) {
            CRON -> CronUtils.nextExecution(scheduleConfig!!, now)
            FIX_RATE -> now
            FIX_DELAY -> now
            ONE_TIME -> parseLocalDateTime(scheduleConfig)
        }
    }

    fun updateNextScheduleTimeWhenNoAvailableWorker() {
        // 固定延迟的调度模式由于调度取消无法更新上次完成时间, 所以用本次调度时间为基准设置下次调度时间
        if (scheduleType == FIX_DELAY) {
            if (this.nextScheduleAt!! < LocalDateTime.now()) {
                this.nextScheduleAt = LocalDateTime.now()
            }
            this.nextScheduleAt = this.nextScheduleAt!!.plusSeconds(this.scheduleConfig!!.toLong())
        } else {
            this.updateNextScheduleTime()
        }
    }

    fun updateNextScheduleTime(
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        validScheduleConfig()
        if (this.nextScheduleAt == null) {
            initNextScheduleTime()
            return
        }
        val nextScheduleTime = when (scheduleType!!) {
            CRON -> {
                val next = CronUtils.nextExecution(scheduleConfig!!, nextScheduleAt!!)
                // 如果服务下线了一段时间, 需要以当前时间来修正下次执行时间
                if (next < now) {
                    CronUtils.nextExecution(scheduleConfig!!, now)
                } else {
                    next
                }
            }

            FIX_RATE -> {
                val next = nextScheduleAt!!.plusSeconds(scheduleConfig!!.toLong())
                // 如果服务下线了一段时间, 需要以当前时间来修正下次执行时间
                if (next < now) {
                    now
                } else {
                    next
                }
            }

            FIX_DELAY -> if (lastCompletedAt == null) {
                now
            } else {
                lastCompletedAt!!.plusSeconds(scheduleConfig!!.toLong())
            }

            ONE_TIME -> nextScheduleAt
        }
        this.nextScheduleAt = nextScheduleTime
    }

    private fun parseLocalDateTime(scheduleConfig: String?): LocalDateTime {
        val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.parse(scheduleConfig!!, pattern)
    }

    fun validScheduleConfig() {
        if (scheduleConfig.isNullOrBlank()) {
            throw BizException("调度配置不能为空")
        }
        when (scheduleType) {
            CRON -> {
                if (CronUtils.isValidCron(scheduleConfig!!).not()) {
                    throw BizException("非法cron表达式. 当前值=$scheduleConfig")
                }
            }

            FIX_RATE, FIX_DELAY -> {
                if (isPositiveNumber(scheduleConfig!!).not()) {
                    throw BizException("调度配置不是正整数. 当前值=$scheduleConfig")
                }
            }

            ONE_TIME -> {
                if (isDateTimeText(scheduleConfig).not()) {
                    throw BizException("调度配置不是 'yyyy-MM-dd HH:mm:ss'格式. 当前值=$scheduleConfig")
                }
            }

            null -> throw BizException("调度类型不能为null")
        }
    }

    private fun isPositiveNumber(s: String): Boolean {
        val number = try {
            s.toLong()
        } catch (_: NumberFormatException) {
            return false
        }
        return number > 0
    }

    private fun isDateTimeText(text: String?): Boolean {
        try {
            parseLocalDateTime(text)
            return true
        } catch (_: Exception) {
            return false
        }
    }
}