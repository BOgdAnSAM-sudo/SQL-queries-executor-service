package com.test_task.jetbrains_internship_test_task.server.controller;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.service.QueryExecutionService;
import com.test_task.jetbrains_internship_test_task.server.service.StoredQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final StoredQueryService queryService;
    private final QueryExecutionService executionService;

    public QueryController(StoredQueryService queryService, QueryExecutionService executionService) {
        this.queryService = queryService;
        this.executionService = executionService;
    }

    @PostMapping("/queries")
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
    public ResponseEntity<List<List<Object>>> executeQuery(@RequestParam Long queryId) {
        List<List<Object>> result = executionService.executeQueryById(queryId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/execute_query")
    public ResponseEntity<List<List<Object>>> executeQuery(@RequestBody String query) {
        List<List<Object>> result = executionService.executeQuery(query);
        return ResponseEntity.ok(result);
    }
}
