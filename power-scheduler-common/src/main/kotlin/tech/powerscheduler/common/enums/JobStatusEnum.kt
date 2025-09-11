package tech.powerscheduler.common.enums

import tech.powerscheduler.common.annotation.Metadata

/**
 * 任务状态枚举
 *
 * @author grayrat
 * @since 2025/5/18
 */
@Metadata(label = "任务状态", code = "JobStatusEnum")
enum class JobStatusEnum(
    override val label: String
) : BaseEnum {

    /**
     * 待调度
     */
    WAITING_SCHEDULE("待调度"),

    /**
     * 待分发
     */
    WAITING_DISPATCH("待分发"),

    /**
     * 排队中
     */
    PENDING("排队中"),

    /**
     * 执行中
     */
    PROCESSING("执行中"),

    /**
     * 失败
     */
    FAILED("失败"),

    /**
     * 成功
     */
    SUCCESS("成功"),
    ;

    override val code = this.name

    companion object {
        /**
         * 未完成状态集合
         */
        val UNCOMPLETED_STATUSES = setOf(
            WAITING_SCHEDULE,
            WAITING_DISPATCH,
            PENDING,
            PROCESSING,
        )

        /**
         * 已完成状态集合
         */
        val COMPLETED_STATUSES = setOf(
            SUCCESS,
            FAILED,
        )

        fun from(taskStatusEnum: TaskStatusEnum): JobStatusEnum {
            return when (taskStatusEnum) {
                TaskStatusEnum.WAITING_DISPATCH -> WAITING_DISPATCH
                TaskStatusEnum.DISPATCHING, TaskStatusEnum.PENDING -> PENDING
                TaskStatusEnum.PROCESSING -> PROCESSING
                TaskStatusEnum.FAILED -> FAILED
                TaskStatusEnum.SUCCESS -> SUCCESS
            }
        }
    }
}