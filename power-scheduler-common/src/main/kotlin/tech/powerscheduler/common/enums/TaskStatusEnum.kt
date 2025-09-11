package tech.powerscheduler.common.enums

enum class TaskStatusEnum(
    override val label: String
) : BaseEnum {
    /**
     * 待分发
     */
    WAITING_DISPATCH("待分发"),

    /**
     * 分发中
     */
    DISPATCHING("分发中"),

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
    }
}