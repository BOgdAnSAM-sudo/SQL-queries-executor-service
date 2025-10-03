package com.test_task.jetbrains_internship_test_task.server.controller;

import com.test_task.jetbrains_internship_test_task.entity.QueryExecutionJob;
import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.repository.QueryExecutionJobRepository;
import com.test_task.jetbrains_internship_test_task.server.service.QueryExecutionService;
import com.test_task.jetbrains_internship_test_task.server.service.StoredQueryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final StoredQueryService queryService;
    private final QueryExecutionService executionService;
    private final QueryExecutionJobRepository jobRepository;

    public QueryController(StoredQueryService queryService, QueryExecutionService executionService, QueryExecutionJobRepository jobRepository) {
        this.queryService = queryService;
        this.executionService = executionService;
        this.jobRepository = jobRepository;
    }

    @PostMapping(value = "/queries", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Map<String, Long>> storeQuery(@RequestBody String query) {
        StoredQuery storedQuery = queryService.addQuery(query);
        Map<String, Long> response = new HashMap<>();
        response.put("id", storedQuery.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/queries")
    public ResponseEntity<List<StoredQuery>> getAllQueries() {
        List<StoredQuery> queries = queryService.getAllQueries();
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestParam Long queryId) {
        StoredQuery query = queryService.getQueryById(queryId).orElseThrow(() -> new RuntimeException("Query not found"));

        QueryExecutionJob newJob = new QueryExecutionJob();
        newJob.setSourceQueryId(queryId);
        newJob.setStatus(QueryExecutionJob.JobStatus.PENDING);
        QueryExecutionJob savedJob = jobRepository.save(newJob);

        executionService.executeQuery(savedJob.getId(), query.getQuery());

        // Create the response body
        Map<String, Object> response = Map.of(
                "jobId", savedJob.getId().toString(),
                "status", "PENDING",
                "message", "Query execution started. Check status endpoint for progress."
        );

        // Return 202 Accepted with a link to the status endpoint
        return ResponseEntity.accepted()
                .location(URI.create("/execution/" + savedJob.getId() + "/status"))
                .body(response);
    }

    @GetMapping("/execution/{jobId}/status")
    public ResponseEntity<?> getStatus(@PathVariable Long jobId) {
        QueryExecutionJob job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        return ResponseEntity.ok(Map.of("jobId", job.getId(), "status", job.getStatus()));
    }

    @GetMapping("/execution/{jobId}/result")
    public ResponseEntity<?> getResult(@PathVariable Long jobId) {
        QueryExecutionJob job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getStatus() != QueryExecutionJob.JobStatus.COMPLETED) {
            return ResponseEntity.ok(Map.of("status", job.getStatus(), "message", "Result not yet available."));
        }

        return ResponseEntity.ok(Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "result", job.getResult()
        ));
    }
}
