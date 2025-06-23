package tech.powerscheduler.server.domain.job

import tech.powerscheduler.common.enums.*
import tech.powerscheduler.common.enums.ExecuteModeEnum.*
import tech.powerscheduler.common.enums.ScheduleTypeEnum.*
import tech.powerscheduler.common.exception.BizException
import tech.powerscheduler.server.domain.appgroup.AppGroup
import tech.powerscheduler.server.domain.utils.CronUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 任务信息
 *
 * @author grayrat
 * @since 2025/4/16
 */
class JobInfo {

    /**
     * 应用分组信息
     */
    var appGroup: AppGroup? = null

    /**
     * 主键
     */
    var id: JobId? = null

    /**
     * 应用编码
     */
    @Deprecated(message = "replace with appGroup")
    var appCode: String? = null

    /**
     * 任务名称
     */
    var jobName: String? = null

    /**
     * 任务描述
     */
    var jobDesc: String? = null

    /**
     * 任务类型
     */
    var jobType: JobTypeEnum? = null

    /**
     * 调度类型
     */
    var scheduleType: ScheduleTypeEnum? = null

    /**
     * 调度配置
     */
    var scheduleConfig: String? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 执行模式
     */
    var executeMode: ExecuteModeEnum? = null

    /**
     * 执行参数
     */
    var executeParams: String? = null

    /**
     * 下次执行时间
     */
    var nextScheduleAt: LocalDateTime? = null

    /**
     * 任务启用状态
     */
    var enabled: Boolean? = null

    /**
     * 任务并发数
     */
    var maxConcurrentNum: Int? = null

    /**
     * 脚本类型
     */
    var scriptType: ScriptTypeEnum? = null

    /**
     * 脚本源代码
     */
    var scriptCode: String? = null

    /**
     * 最大重试次数
     */
    var maxAttemptCnt: Int? = null

    /**
     * 重试间隔(s)
     */
    var attemptInterval: Int? = null

    /**
     * 子任务最大重试次数
     */
    var taskMaxAttemptCnt: Int? = null

    /**
     * 子任务重试间隔(s)
     */
    var taskAttemptInterval: Int? = null

    /**
     * 优先级
     */
    var priority: Int? = null

    /**
     * 上次完成时间
     */
    var lastCompletedAt: LocalDateTime? = null

    /**
     * 调度器地址
     */
    var schedulerAddress: String? = null

    /**
     * 保留策略
     */
    var retentionPolicy: RetentionPolicyEnum? = null

    /**
     * 保留值
     */
    var retentionValue: Int? = null

    /**
     * 创建人
     */
    var createdBy: String? = null

    /**
     * 创建时间
     */
    var createdAt: LocalDateTime? = null

    /**
     * 修改人
     */
    var updatedBy: String? = null

    /**
     * 修改时间
     */
    var updatedAt: LocalDateTime? = null

    fun createInstance(): JobInstance {
        return JobInstance().also {
            it.appGroup = this.appGroup
            it.sourceId = this.id!!.toSourceId()
            it.sourceType = JobSourceTypeEnum.JOB
            it.jobName = this.jobName
            it.jobType = this.jobType
            it.processor = this.processor
            it.scheduleType = this.scheduleType
            it.executeMode = this.executeMode
            it.executeParams = this.executeParams
            it.scriptType = this.scriptType
            it.scriptCode = this.scriptCode

            it.jobStatus = JobStatusEnum.WAITING_SCHEDULE
            it.dataTime = this.nextScheduleAt
            it.attemptCnt = 0
            it.maxAttemptCnt = this.maxAttemptCnt ?: 1
            it.attemptInterval = this.attemptInterval
            it.taskMaxAttemptCnt = this.taskMaxAttemptCnt
            it.taskAttemptInterval = this.taskAttemptInterval
            it.priority = this.priority
            it.scheduleAt = this.nextScheduleAt
        }
    }

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

    fun updateNextScheduleTime(
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        validate()
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

    fun validate() {
        validScheduleConfig()
        validateExecuteConfig()
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

    private fun validateExecuteConfig() {
        if (this.jobType == JobTypeEnum.SCRIPT && this.executeMode in arrayOf(MAP, MAP_REDUCE)) {
            throw BizException("脚本任务不能使用[$executeMode]模式")
        }
        when (executeMode) {
            SINGLE -> {}

            BROADCAST, MAP, MAP_REDUCE -> {
                if (taskMaxAttemptCnt == null || taskMaxAttemptCnt!! < 0) {
                    throw BizException("子任务最大重试次数必须为非负数")
                }
                if (taskAttemptInterval == null || taskAttemptInterval!! < 0) {
                    throw BizException("子任务重试间隔必须为非负数")
                }
            }

            null -> throw BizException("执行模式不能为null")
        }
    }

    private fun isDateTimeText(text: String?): Boolean {
        try {
            parseLocalDateTime(text)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun parseLocalDateTime(scheduleConfig: String?): LocalDateTime {
        val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDateTime.parse(scheduleConfig!!, pattern)
    }

    private fun isPositiveNumber(s: String): Boolean {
        val number = try {
            s.toLong()
        } catch (_: NumberFormatException) {
            return false
        }
        return number > 0
    }
}