package tech.powerscheduler.worker.sample.springboot.processor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.powerscheduler.worker.processor.JavaProcessor;
import tech.powerscheduler.worker.processor.ProcessResult;
import tech.powerscheduler.worker.task.TaskContext;

/**
 * @author grayrat
 * @since 2025/12/7
 */
public class MayFailJobProcessorDemo extends JavaProcessor {

    @Override
    public @Nullable ProcessResult process(@NotNull TaskContext context) throws Exception {
        if (System.currentTimeMillis() / 2 == 0) {
            throw new RuntimeException("unluck, failed");
        }
        return ProcessResult.success();
    }
}
