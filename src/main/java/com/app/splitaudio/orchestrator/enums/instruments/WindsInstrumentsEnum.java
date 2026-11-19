package com.app.splitaudio.orchestrator.enums.instruments;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WindsInstrumentsEnum {

    WINDS("Woodwinds"),
    NO_WINDS("No Woodwinds");

    @JsonValue
    private final String value;
}
