package com.app.splitaudio.exceptions;

import com.app.splitaudio.constants.Constants;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        super(Constants.Exceptions.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(Constants.Exceptions.RESOURCE_NOT_FOUND + ": " + message);
    }
}
