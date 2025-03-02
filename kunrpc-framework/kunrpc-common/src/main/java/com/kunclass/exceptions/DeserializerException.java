package com.kunclass.exceptions;

public class DeserializerException extends RuntimeException {
    public DeserializerException() {
    }
    public DeserializerException(Throwable cause) {
        super(cause);
    }
    public DeserializerException(String message) {
        super(message);
    }
}
