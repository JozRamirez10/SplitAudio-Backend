package com.app.splitaudio.orchestrator.enums.instruments;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GuitarTypeInstrumentsEnum {

    ELECTRIC("electric_guitar"),
    ACOUSTIC("acoustic_guitar");

    @JsonValue
    private final String value;
}
