package com.app.splitaudio.orchestrator.stemprocessors;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.app.splitaudio.orchestrator.clients.SplitAudioClient;
import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public abstract class AbstractStemProcessor implements StemProcessor {

    protected final SplitAudioClient splitAudioClient;
    private final String strategyName;
    private final String stemKeyword;

    protected AbstractStemProcessor(SplitAudioClient splitAudioClient, String strategyName,
        String stemKeyword) {

        this.splitAudioClient = splitAudioClient;
        this.strategyName = strategyName;
        this.stemKeyword = stemKeyword;
    }
    
    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public String getTargetStemKeyword() {
        return stemKeyword;
    }

    @Override
    public List<String> execute(String stemPath, OrchestratorDtoRequest request, 
        AtomicBoolean killSwitch) {
        
        checkKillSwitch(killSwitch);

        log.info("*** [{}] Separation stem from: {}", strategyName, stemPath);

        List<String> outputPaths = performApiCall(stemPath, request);

        log.info("*** [{}] Successfully separation", strategyName);

        return outputPaths;
    }

    protected abstract List<String> performApiCall(String stemPath, OrchestratorDtoRequest request);
}
