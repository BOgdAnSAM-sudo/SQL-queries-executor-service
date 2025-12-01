package com.executor.server.service;

public class StoredQueryException extends RuntimeException{
    public StoredQueryException(String message) {
        super(message);
    }

    public StoredQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
