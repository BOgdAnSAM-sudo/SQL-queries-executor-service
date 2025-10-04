package com.test_task.jetbrains_internship_test_task.server.service;

public class QueryExecutionJobException extends RuntimeException{
    public QueryExecutionJobException(String message) {
        super(message);
    }

    public QueryExecutionJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
