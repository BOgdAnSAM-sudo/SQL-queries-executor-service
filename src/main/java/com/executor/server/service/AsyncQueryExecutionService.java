package com.executor.server.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Serves as wrapper for {@link QueryExecutionService} for async execution of queries
 */
@Service
public class AsyncQueryExecutionService {

    private final QueryExecutionService executionService;

    public AsyncQueryExecutionService(QueryExecutionService queryExecutionService) {
        this.executionService = queryExecutionService;
    }

    @Async
    public void executeQuery(Long jobId) {
        executionService.executeQuery(jobId);
    }

}