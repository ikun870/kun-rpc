package com.kunclass.exceptions;

public class LoadBalancerException extends RuntimeException {
    public LoadBalancerException(String message) {
        super(message);
    }
}
