package tech.powerscheduler.server.application.dto.response

import java.time.LocalDateTime

class WorkflowNodeInstanceDetailResponseDTO {
    /**
     * 工作流节点实例id
     */
    var id: Long? = null

    /**
     * 子节点实例id列表
     */
    var childrenIds: List<Long>? = null

    /**
     * 父节点实例di列表
     */
    var parentIds: List<Long>? = null

    /**
     * 节点编码
     */
    var nodeCode: String? = null

    /**
     * 节点实例编号
     */
    var nodeInstanceCode: String? = null

    /**
     * 节点名称
     */
    var name: String? = null

    /**
     * 任务类型
     */
    var jobType: EnumDTO? = null

    /**
     * 任务处理器
     */
    var processor: String? = null

    /**
     * 任务状态
     */
    var status: EnumDTO? = null

    /**
     * 执行模式
     */
    var executeMode: EnumDTO? = null

    /**
     * 任务参数
     */
    var executeParams: String? = null

    /**
     * 脚本类型
     */
    var scriptType: EnumDTO? = null

    /**
     * 脚本源代码
     */
    var scriptCode: String? = null

    /**
     * 数据时间
     */
    var dataTime: LocalDateTime? = null

    /**
     * 开始时间
     */
    var startAt: LocalDateTime? = null

    /**
     * 结束时间
     */
    var endAt: LocalDateTime? = null

    /**
     * Worker地址（ip:host）
     */
    var workerAddress: String? = null

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
}