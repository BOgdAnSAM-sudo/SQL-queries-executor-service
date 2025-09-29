package com.test_task.jetbrains_internship_test_task.server.service;

public class StoredQueryException extends RuntimeException{
    public StoredQueryException(String message) {
        super(message);
    }

    public StoredQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
