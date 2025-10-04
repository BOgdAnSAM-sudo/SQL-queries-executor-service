package com.test_task.jetbrains_internship_test_task.server.controller;

import com.test_task.jetbrains_internship_test_task.entity.QueryExecutionJob;
import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.service.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final StoredQueryService queryService;
    private final QueryExecutionService executionService;
    private final QueryExecutionJobService jobService;

    public QueryController(StoredQueryService queryService, QueryExecutionService executionService, QueryExecutionJobService jobService) {
        this.queryService = queryService;
        this.executionService = executionService;
        this.jobService = jobService;
    }

    @PostMapping(value = "/queries", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Map<String, Long>> storeQuery(@RequestBody String query) {
        StoredQuery storedQuery = queryService.addQuery(query);

        return ResponseEntity.created(URI.create("/api/queries/" + storedQuery.getId()))
                .body(Map.of("id", storedQuery.getId()));
    }

    @GetMapping("/queries")
    public ResponseEntity<List<StoredQuery>> getAllQueries() {
        List<StoredQuery> queries = queryService.getAllQueries();
        return ResponseEntity.ok(queries);
    }

    @PostMapping("/queries/{queryId}/execute")
    public ResponseEntity<?> executeQuery(@PathVariable Long queryId) {
        queryService.getQueryById(queryId).orElseThrow(() -> new StoredQueryException("Query not found"));

        if (jobService.getJobById(queryId).isPresent()){
            throw new QueryExecutionJobException("Job already exists");
        }

        QueryExecutionJob savedJob = jobService.addJob(queryId);

        executionService.executeQuery(savedJob.getId());

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

    @GetMapping("/executions/{jobId}/status")
    public ResponseEntity<?> getStatus(@PathVariable Long jobId) {
        QueryExecutionJob job = jobService.getJobById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        return ResponseEntity.ok(Map.of("jobId", job.getId(), "status", job.getStatus()));
    }

    @GetMapping("/executions/{jobId}/result")
    public ResponseEntity<?> getResult(@PathVariable Long jobId) {
        QueryExecutionJob job = jobService.getJobById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));

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
