package com.app.splitaudio.orchestrator.stemprocessors;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.splitaudio.orchestrator.clients.SplitAudioClient;
import com.app.splitaudio.orchestrator.dtos.requests.OrchestratorDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitAudioDtoRequest;
import com.app.splitaudio.orchestrator.enums.instruments.GuitarRoleInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.GuitarTypeInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.TrackInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.modes.GuitarModeEnum;

@Component 
public class GuitarsStemProcessor extends AbstractStemProcessor {
    
    private static final String STRATEGY_NAME = "SPLIT-GUITARS";

    public GuitarsStemProcessor(SplitAudioClient splitAudioClient) {
        super(splitAudioClient, STRATEGY_NAME, TrackInstrumentsEnum.GUITAR.getValue());
    }

    @Override
    public boolean isApplicable(OrchestratorDtoRequest request) {
        return request.guitarMode() == GuitarModeEnum.BY_ROLE 
            || request.guitarMode() == GuitarModeEnum.BY_TYPE;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    protected List<String> performApiCall(String stemPath, OrchestratorDtoRequest request) {
        if (request.guitarMode() == GuitarModeEnum.BY_ROLE) {
            return splitAudioClient.processRoleGuitars(
                new SplitAudioDtoRequest<>(stemPath, List.<GuitarRoleInstrumentsEnum>of())
            ).outputPaths();
        }
        return splitAudioClient.processTypeGuitars(
            new SplitAudioDtoRequest<>(stemPath, List.<GuitarTypeInstrumentsEnum>of())
        ).outputPaths();
    }
}
