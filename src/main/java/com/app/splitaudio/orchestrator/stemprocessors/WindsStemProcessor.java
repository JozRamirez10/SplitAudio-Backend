package com.app.splitaudio.orchestrator.stemprocessors;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.splitaudio.orchestrator.clients.SplitAudioClient;
import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitAudioDtoRequest;
import com.app.splitaudio.orchestrator.enums.instruments.TrackInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.WindsInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.modes.WindsModeEnum;

@Component 
public class WindsStemProcessor extends AbstractStemProcessor {

    private static final String STRATEGY_NAME = "SPLIT-WINDS";

    public WindsStemProcessor(SplitAudioClient splitAudioClient) {
        super(splitAudioClient, STRATEGY_NAME, TrackInstrumentsEnum.OTHER.getValue());
    }

    @Override
    public boolean isApplicable(OrchestratorDtoRequest request) {
        return request.windsMode() == WindsModeEnum.INDIVIDUAL;
    }

    @Override
    protected List<String> performApiCall(String stemPath, OrchestratorDtoRequest request) {
        return splitAudioClient.processWinds(
            new SplitAudioDtoRequest<>(stemPath, List.<WindsInstrumentsEnum>of())
        ).outputPaths();
    }
}