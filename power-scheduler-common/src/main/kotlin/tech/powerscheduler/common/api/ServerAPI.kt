package tech.powerscheduler.common.api

/**
 * server端API前缀
 *
 * @author grayrat
 * @since 2025/5/14
 */
const val SERVER_API_PREFIX = "internal-api/v1/worker"

/**
 * server端健康检查API
 *
 * @author grayrat
 * @since 2025/5/14
 */
const val CHECK_HEALTH_API = "checkHealth"

/**
 * worker注册API
 *
 * @author grayrat
 * @since 2025/5/14
 */
const val REGISTER_API = "register"

/**
 * worker心跳API
 *
 * @author grayrat
 * @since 2025/5/14
 */
const val HEARTBEAT_API = "heartbeat"

/**
 * worker下线API
 *
 * @author grayrat
 * @since 2025/5/14
 */
const val UNREGISTER_API = "unregister"

/**
 * 任务进度上报API
 *
 * @author grayrat
 * @since 2025/5/14
 */
const val REPORT_PROGRESS_API = "reportProgress"

/**
 * 系统指标上报API
 *
 * @author grayrat
 * @since 2025/6/16
 */
const val REPORT_METRICS_API = "reportMetrics"

/**
 * 拉取任务结果
 *
 * @author grayrat
 * @since 2025/6/19
 */
const val FETCH_TASK_RESULT = "fetchTaskResult"
