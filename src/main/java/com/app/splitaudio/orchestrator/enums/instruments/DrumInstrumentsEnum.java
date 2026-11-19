package com.app.splitaudio.orchestrator.enums.instruments;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrumInstrumentsEnum {

    KICK("kick"),
    SNARE("snare"),
    TOMS("toms"),
    RIDE("ride"),
    CRASH("crash"),
    HI_HATS("hh");

    @JsonValue
    private final String value;
}
