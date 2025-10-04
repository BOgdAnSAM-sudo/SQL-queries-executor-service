package com.test_task.jetbrains_internship_test_task.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test_task.jetbrains_internship_test_task.entity.QueryExecutionJob;
import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.repository.QueryExecutionJobRepository;
import com.test_task.jetbrains_internship_test_task.server.repository.QueryExecutionRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QueryExecutionService {

    private final QueryExecutionRepository queryExecutionRepository;
    private final QueryExecutionJobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final StoredQueryService storedQueryService;

    public QueryExecutionService(QueryExecutionRepository queryExecutionRepository, QueryExecutionJobRepository jobRepository, ObjectMapper objectMapper, StoredQueryService storedQueryService) {
        this.queryExecutionRepository = queryExecutionRepository;
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
        this.storedQueryService = storedQueryService;
    }


    @Async
    @Transactional
    public void executeQuery(Long jobId) {
        QueryExecutionJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(QueryExecutionJob.JobStatus.RUNNING);

        Optional<StoredQuery> storedQuery = storedQueryService.getQueryById(job.getSourceQueryId());

        if (storedQuery.isEmpty()) {
            String errorMessage = "Source query not found with ID: " + job.getSourceQueryId();
            job.setStatus(QueryExecutionJob.JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            throw new RuntimeException(errorMessage);
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