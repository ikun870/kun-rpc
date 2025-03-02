package com.kunclass.exceptions;

public class SerializerException extends RuntimeException {
    public SerializerException() {
    }

    public SerializerException(Throwable cause) {
        super(cause);
    }

    public SerializerException(String message) {
        super(message);
    }
}
