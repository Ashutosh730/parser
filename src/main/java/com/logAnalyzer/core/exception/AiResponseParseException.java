package com.logAnalyzer.core.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

public class AiResponseParseException extends Throwable {
    public AiResponseParseException(String s, JsonProcessingException e) {
    }
}
