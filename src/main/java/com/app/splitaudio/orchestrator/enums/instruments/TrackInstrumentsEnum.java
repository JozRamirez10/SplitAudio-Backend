package com.app.splitaudio.orchestrator.enums.instruments;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackInstrumentsEnum {

    DRUMS("drums"),
    BASS("bass"),
    OTHER("other"),
    PIANO("piano"),
    GUITAR("guitar"),
    VOCALS("vocals");

    @JsonValue
    private final String value;
}