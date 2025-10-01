package com.test_task.jetbrains_internship_test_task.server.service;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.repository.QueryExecutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QueryExecutionService {

    private final QueryExecutionRepository queryExecutionRepository;
    private final StoredQueryService storedQueryService;

    public QueryExecutionService(QueryExecutionRepository queryExecutionRepository, StoredQueryService storedQueryService) {
        this.queryExecutionRepository = queryExecutionRepository;
        this.storedQueryService = storedQueryService;
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

    public List<List<Object>> executeQueryById(Long id){
        Optional<StoredQuery> storedQuery = storedQueryService.getQueryById(id);
        return storedQuery.map(query -> executeQuery(query.getQuery())).orElse(null);
    }
}