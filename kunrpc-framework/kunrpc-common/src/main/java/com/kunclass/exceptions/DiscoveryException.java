package com.kunclass.exceptions;

public class DiscoveryException extends RuntimeException {
    public DiscoveryException() {
    }

    public DiscoveryException(Throwable cause) {
        super(cause);
    }

    public DiscoveryException(String msg) {
        super(msg);
    }
}
