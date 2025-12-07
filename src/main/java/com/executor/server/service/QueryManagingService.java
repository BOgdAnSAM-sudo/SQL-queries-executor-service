package com.executor.server.service;

import com.executor.entity.QueryExecutionJob;
import com.executor.entity.StoredQuery;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QueryManagingService {
    private final QueryExecutionService queryExecutionService;
    private final StoredQueryService storedQueryService;
    private final QueryExecutionJobService jobService;

    public QueryManagingService(QueryExecutionService queryExecutionService, StoredQueryService storedQueryService, QueryExecutionJobService jobService) {
        this.queryExecutionService = queryExecutionService;
        this.storedQueryService = storedQueryService;
        this.jobService = jobService;
    }

    protected void executeQuery(Long jobId) {
        jobService.markJobRunning(jobId);

        QueryExecutionJob job = jobService.getJobById(jobId).orElseThrow();
        Optional<StoredQuery> storedQuery = storedQueryService.getQueryById(job.getSourceQueryId());

        if (storedQuery.isEmpty()) {
            jobService.markJobFailed(jobId, "Source query not found");
            return;
        }

        String query = storedQuery.get().getQuery();

        try {
            String result = queryExecutionService.cacheableQueryExecution(query);
            jobService.markJobCompleted(jobId, result);
        } catch (Exception e) {
            jobService.markJobFailed(jobId, e.getMessage());
        }
    }

}
