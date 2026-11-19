package com.app.splitaudio.orchestrator.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.app.splitaudio.orchestrator.dtos.requests.CleanupDtoRequest;
import com.app.splitaudio.orchestrator.dtos.requests.SplitAudioDtoRequest;
import com.app.splitaudio.orchestrator.dtos.responses.CleanupDtoResponse;
import com.app.splitaudio.orchestrator.dtos.responses.SplitAudioDtoResponse;
import com.app.splitaudio.orchestrator.enums.instruments.DrumInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.GuitarRoleInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.GuitarTypeInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.TrackInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.VocalInstrumentsEnum;
import com.app.splitaudio.orchestrator.enums.instruments.WindsInstrumentsEnum;

@FeignClient(name = "SplitAudioApiClient", url = "${python.api.splitaudio.url}")
public interface SplitAudioClient {

    @PostMapping("/api/process-tracks")
    SplitAudioDtoResponse processTracks(@RequestBody SplitAudioDtoRequest<TrackInstrumentsEnum> request);

    @PostMapping("/api/process-drums")
    SplitAudioDtoResponse processDrums(@RequestBody SplitAudioDtoRequest<DrumInstrumentsEnum> request);

    @PostMapping("/api/process-vocals")
    SplitAudioDtoResponse processVocals(@RequestBody SplitAudioDtoRequest<VocalInstrumentsEnum> request);

    @PostMapping("/api/process-winds")
    SplitAudioDtoResponse processWinds(@RequestBody SplitAudioDtoRequest<WindsInstrumentsEnum> request);

    @PostMapping("/api/process-role-guitars")
    SplitAudioDtoResponse processRoleGuitars(@RequestBody SplitAudioDtoRequest<GuitarRoleInstrumentsEnum> request);

    @PostMapping("/api/process-type-guitars")
    SplitAudioDtoResponse processTypeGuitars(@RequestBody SplitAudioDtoRequest<GuitarTypeInstrumentsEnum> request);

    @DeleteMapping("/api/cleanup")
    CleanupDtoResponse cleanup(@RequestBody CleanupDtoRequest request);
}
