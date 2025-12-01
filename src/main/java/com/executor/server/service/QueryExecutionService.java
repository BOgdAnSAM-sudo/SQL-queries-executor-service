package com.executor.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.executor.entity.QueryExecutionJob;
import com.executor.entity.StoredQuery;
import com.executor.server.repository.QueryExecutionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QueryExecutionService {

    private final QueryExecutionRepository queryExecutionRepository;
    private final ObjectMapper objectMapper;
    private final StoredQueryService storedQueryService;
    private final QueryExecutionJobService jobService;

    public QueryExecutionService(QueryExecutionRepository queryExecutionRepository, ObjectMapper objectMapper, StoredQueryService storedQueryService, QueryExecutionJobService jobService) {
        this.queryExecutionRepository = queryExecutionRepository;
        this.objectMapper = objectMapper;
        this.storedQueryService = storedQueryService;
        this.jobService = jobService;
    }

    @Async
    @Transactional
    public void executeQuery(Long jobId) {
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
            List<Map<String, Object>> queryResult = queryExecutionRepository.executeNativeQuery(query);

            List<List<Object>> formattedResult = convertResultToList(queryResult);

//            Store the result as a JSON string
            String resultJson = objectMapper.writeValueAsString(formattedResult);

            job.setResult(resultJson);
            job.setStatus(QueryExecutionJob.JobStatus.COMPLETED);

        } catch (Exception e) {
//            If anything goes wrong, mark as FAILED
            job.setStatus(QueryExecutionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        }
    }

    private List<List<Object>> convertResultToList(List<Map<String, Object>> result) {
        return result.stream()
                .map(row -> row.values().stream().toList())
                .toList();
    }

}