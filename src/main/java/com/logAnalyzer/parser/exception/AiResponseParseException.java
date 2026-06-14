package com.logAnalyzer.parser.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

public class AiResponseParseException extends Throwable {
    public AiResponseParseException(String s, JsonProcessingException e) {
    }
}
