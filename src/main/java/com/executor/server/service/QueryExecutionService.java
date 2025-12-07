package com.executor.server.service;

import com.executor.server.repository.QueryExecutionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryExecutionService {
    private final QueryExecutionRepository queryExecutionRepository;
    private final ObjectMapper objectMapper;

    public QueryExecutionService(QueryExecutionRepository queryExecutionRepository, ObjectMapper objectMapper) {
        this.queryExecutionRepository = queryExecutionRepository;
        this.objectMapper = objectMapper;
    }

    @Cacheable("queryResults")
    public String cacheableQueryExecution(String query) throws JsonProcessingException {
        List<Map<String, Object>> queryResult = queryExecutionRepository.executeNativeQuery(query);

        List<List<Object>> formattedResult = convertResultToList(queryResult);

        return objectMapper.writeValueAsString(formattedResult);
    }

    private List<List<Object>> convertResultToList(List<Map<String, Object>> result) {
        return result.stream()
                .map(row -> row.values().stream().toList())
                .toList();
    }
}
