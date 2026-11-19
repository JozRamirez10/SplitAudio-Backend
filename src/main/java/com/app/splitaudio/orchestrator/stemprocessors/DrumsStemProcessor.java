package com.app.splitaudio.orchestrator.stemprocessors;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.splitaudio.orchestrator.clients.SplitAudioClient;
import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitAudioDtoRequest;
import com.app.splitaudio.orchestrator.enums.instruments.DrumInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.TrackInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.modes.DrumsModeEnum;

@Component 
public class DrumsStemProcessor extends AbstractStemProcessor {

    private static final String STRATEGY_NAME = "SPLIT-DRUMS";

    public DrumsStemProcessor(SplitAudioClient splitAudioClient) {
        super(splitAudioClient, STRATEGY_NAME, TrackInstrumentsEnum.DRUMS.getValue());
    }

    @Override
    public boolean isApplicable(OrchestratorDtoRequest request) {
        return request.drumsMode() == DrumsModeEnum.INDIVIDUAL;
    }

    @Override
    protected List<String> performApiCall(String stemPath, OrchestratorDtoRequest request) {
        return splitAudioClient.processDrums(
            new SplitAudioDtoRequest<>(stemPath, List.<DrumInstrumentsEnum>of())
        ).outputPaths();
    }
}
