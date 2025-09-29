package com.test_task.jetbrains_internship_test_task.server.service;

import com.test_task.jetbrains_internship_test_task.server.repository.QueryExecutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final QueryExecutionRepository queryExecutionRepository;

    public QueryExecutionService(QueryExecutionRepository queryExecutionRepository) {
        this.queryExecutionRepository = queryExecutionRepository;
    }

    public List<List<Object>> executeQuery(String query) {
        List<Map<String, Object>> result = queryExecutionRepository.executeNativeQuery(query);
        return convertResultToList(result);
    }

    private List<List<Object>> convertResultToList(List<Map<String, Object>> result) {
        return result.stream()
                .map(row -> row.values().stream().toList())
                .toList();
    }
}