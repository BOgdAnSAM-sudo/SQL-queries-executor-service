package com.executor.server.service;

import com.executor.server.repository.QueryExecutionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryExecutionServiceTest {

    @Mock
    private QueryExecutionRepository queryExecutionRepository;

    // Use @Spy to use the real ObjectMapper logic but still allow verifying/stubbing if needed
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private QueryExecutionService queryExecutionService;

    @Test
    @DisplayName("Should return valid JSON string when query executes successfully")
    void cacheableQueryExecution_Success() throws JsonProcessingException {
        String sqlQuery = "SELECT id, name FROM users";

        // Use LinkedHashMap to ensure deterministic order of values for the test
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("id", 2);
        row2.put("name", "Bob");

        List<Map<String, Object>> mockDbResult = List.of(row1, row2);

        when(queryExecutionRepository.executeNativeQuery(sqlQuery)).thenReturn(mockDbResult);

        String resultJson = null;
        try {
            resultJson = queryExecutionService.cacheableQueryExecution(sqlQuery);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String expectedJson = "[[1,\"Alice\"],[2,\"Bob\"]]";
        assertEquals(expectedJson, resultJson);

        verify(queryExecutionRepository, times(1)).executeNativeQuery(sqlQuery);
    }

    @Test
    @DisplayName("Should return empty JSON array when query returns no results")
    void cacheableQueryExecution_EmptyResult() throws JsonProcessingException {
        String sqlQuery = "SELECT * FROM empty_table";
        when(queryExecutionRepository.executeNativeQuery(sqlQuery)).thenReturn(Collections.emptyList());

        String resultJson = queryExecutionService.cacheableQueryExecution(sqlQuery);

        assertEquals("[]", resultJson);
        verify(queryExecutionRepository).executeNativeQuery(sqlQuery);
    }

    @Test
    @DisplayName("Should propagate JsonProcessingException if serialization fails")
    void cacheableQueryExecution_JsonException() throws JsonProcessingException {
        String sqlQuery = "SELECT * FROM table";
        List<Map<String, Object>> mockDbResult = List.of(Map.of("key", "value"));

        when(queryExecutionRepository.executeNativeQuery(sqlQuery)).thenReturn(mockDbResult);

        doThrow(new JsonProcessingException("Serialization error") {})
                .when(objectMapper).writeValueAsString(any());

        assertThrows(JsonProcessingException.class, () -> {
            queryExecutionService.cacheableQueryExecution(sqlQuery);
        });
    }

}
