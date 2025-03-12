package com.kunclass.exceptions;

public class ResponseException extends RuntimeException {

    private byte code;
    private String message;

    public ResponseException(String message, byte code) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
