package com.executor.server.service;

import com.executor.entity.QueryExecutionJob;
import com.executor.entity.StoredQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    protected void executeQuery(Long jobId){
        QueryExecutionJob job = jobService.getJobById(jobId).orElseThrow();
        job.setStatus(QueryExecutionJob.JobStatus.RUNNING);

        Optional<StoredQuery> storedQuery = storedQueryService.getQueryById(job.getSourceQueryId());

        if (storedQuery.isEmpty()) {
            String errorMessage = "Source query not found with ID: " + job.getSourceQueryId();
            job.setStatus(QueryExecutionJob.JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            throw new QueryExecutionJobException(errorMessage);
        }

        String query = storedQuery.get().getQuery();

        try {
            job.setResult(queryExecutionService.cacheableQueryExecution(query));
            job.setStatus(QueryExecutionJob.JobStatus.COMPLETED);

        } catch (Exception e) {
//            If anything goes wrong, mark as FAILED
            job.setStatus(QueryExecutionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        }
    }

}
