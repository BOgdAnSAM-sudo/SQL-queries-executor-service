package com.executor.server.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Serves as wrapper for {@link QueryManagingService} for async execution of queries
 */
@Service
public class AsyncQueryManagingService {

    private final QueryManagingService executionService;

    public AsyncQueryManagingService(QueryManagingService queryManagingService) {
        this.executionService = queryManagingService;
    }

    @Async
    public void executeQuery(Long jobId) {
        executionService.executeQuery(jobId);
    }

}