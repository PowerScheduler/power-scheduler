package tech.powerscheduler.common.enums

import tech.powerscheduler.common.annotation.Metadata

/**
 * @author grayrat
 * @since 2025/6/25
 */
@Metadata(label = "工作流状态", code = "WorkflowStatusEnum")
enum class WorkflowStatusEnum(
    override val label: String
) : BaseEnum {
    /**
     * 等待
     */
    WAITING("等待"),

    /**
     * 运行中
     */
    RUNNING("运行中"),

    /**
     * 成功
     */
    SUCCESS("成功"),

    /**
     * 失败
     */
    FAILED("失败"),

    /**
     * 取消
     */
    CANCELED("取消"),

    /**
     * 未知(如果出现没有考虑到的分支, 则使用此状态兜底)
     */
    UNKNOWN("未知");
    ;

    override val code = this.name

    companion object {
        fun from(jobStatusEnum: JobStatusEnum): WorkflowStatusEnum {
            return when (jobStatusEnum) {
                JobStatusEnum.WAITING_SCHEDULE, JobStatusEnum.WAITING_DISPATCH,
                JobStatusEnum.DISPATCHING, JobStatusEnum.PENDING, JobStatusEnum.PROCESSING -> RUNNING

                JobStatusEnum.FAILED, JobStatusEnum.UNKNOWN -> FAILED

                JobStatusEnum.SUCCESS -> SUCCESS

                JobStatusEnum.CANCELED -> CANCELED
            }
        }

        /**
         * 未完成状态集合
         */
        val UNCOMPLETED_STATUSES = setOf(
            WAITING,
            RUNNING,
        )

        /**
         * 已完成状态集合
         */
        val COMPLETED_STATUSES = setOf(
            SUCCESS,
            FAILED,
            CANCELED,
            UNKNOWN,
        )
    }
}