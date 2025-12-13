package tech.powerscheduler.server.application.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext

/**
 * @author grayrat
 * @since 2025/6/9
 */
class CoroutineExecutor(
    concurrency: Int,
    private val exceptionHandlePolicy: (Exception) -> Unit = throwPolicy,
) {

    companion object {
        val throwPolicy: (Exception) -> Unit = { e -> throw e }
    }

    private val channel = Channel<Unit>(concurrency)

    fun submit(
        task: () -> Unit
    ) {
        val async = ExecutorCoroutineScope.async {
            try {
                channel.send(Unit)
                task.invoke()
            } catch (e: Exception) {
                exceptionHandlePolicy.invoke(e)
            } finally {
                channel.receive()
            }
        }
    }


    /**
     * 自定义的协程作用领，用于实现结构化并发
     *
     * @author grayrat
     * @since 2025/5/25
     */
    internal object ExecutorCoroutineScope : CoroutineScope {

        /**
         * 用于管理所有协程的生命周期
         */
        private val manager = Job()

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.IO + manager

        /**
         * 取消所有协程任务
         */
        fun cancelJobs() {
            manager.cancel()
        }
    }
}