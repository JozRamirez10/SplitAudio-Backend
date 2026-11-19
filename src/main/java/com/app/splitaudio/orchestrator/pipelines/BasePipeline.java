package com.app.splitaudio.orchestrator.pipelines;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public abstract class BasePipeline {

    private static final String ABORTED_MESSAGE = "Pipeline aborted due to parallel error";

    @FunctionalInterface 
    protected interface PipelineTask<T> {
        T execute(String pipelineName) throws Exception;
    }

    protected <T> CompletableFuture<T> executePipelineAsync(String pipelineName, 
        AtomicBoolean killSwitch, PipelineTask<T> task) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                
                log.info("*** [{}] Starting pipeline...", pipelineName);

                T result = task.execute(pipelineName);

                log.info("*** [{}] Pipeline completed successfully", pipelineName);

                return result;

            } catch (CancellationException e) {
                log.warn("*** [{}] Pipeline aborted by killswitch: {}", pipelineName, 
                e.getMessage());
                throw e;
            } catch (Exception e) {
                killSwitch.set(true);
                log.error("*** [{}] Pipeline failed: {}. Activating killswitch", 
                    pipelineName, e.getMessage());
                throw new CompletionException(e);
            }
        });
    }

    protected <T> T step(AtomicBoolean killSwitch, Supplier<T> action) {
        if (killSwitch.get()) {
            throw new CancellationException(ABORTED_MESSAGE);
        }
        return action.get();
    }

    protected void step(AtomicBoolean killSwitch, Runnable action) {
        if (killSwitch.get()) {
            throw new CancellationException(ABORTED_MESSAGE);
        }
        action.run();
    }

    protected void executeCleanup(String pipelineName, Runnable cleanupAction) {
        try {
            cleanupAction.run();
        } catch (Exception ex) {
            log.warn("*** [{}] Failed to clean local file: {}", pipelineName, 
                ex.getMessage());
        }
    }
}
