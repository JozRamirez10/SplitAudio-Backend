package com.app.splitaudio.orchestrator.stemprocessors;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;

public interface StemProcessor {

    boolean isApplicable(OrchestratorDtoRequest request);

    boolean isAsync();

    String getTargetStemKeyword();

    List<String> execute(String stemPath, OrchestratorDtoRequest request, AtomicBoolean killSwitch);
    
    default void checkKillSwitch(AtomicBoolean killSwitch) {
        if (killSwitch.get()) {
            throw new CancellationException("Pipeline aborted due to parallel error");
        }
    }
}
