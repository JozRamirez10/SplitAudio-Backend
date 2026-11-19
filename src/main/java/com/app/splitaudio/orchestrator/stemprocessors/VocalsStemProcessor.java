package com.app.splitaudio.orchestrator.stemprocessors;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.splitaudio.orchestrator.clients.SplitAudioClient;
import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitAudioDtoRequest;
import com.app.splitaudio.orchestrator.enums.instruments.TrackInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.VocalInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.modes.VocalsModeEnum;

@Component 
public class VocalsStemProcessor extends AbstractStemProcessor {

    private static final String STRATEGY_NAME = "SPLIT-VOCALS";

    public VocalsStemProcessor(SplitAudioClient splitAudioClient) {
        super(splitAudioClient, STRATEGY_NAME, TrackInstrumentsEnum.VOCALS.getValue());
    }

    @Override
    public boolean isApplicable(OrchestratorDtoRequest request) {
        return request.vocalsMode() == VocalsModeEnum.INDIVIDUAL;
    }

    @Override
    protected List<String> performApiCall(String stemPath, OrchestratorDtoRequest request) {
        return splitAudioClient.processVocals(
            new SplitAudioDtoRequest<>(stemPath, List.<VocalInstrumentsEnum>of())
        ).outputPaths();
    }
}