package com.test_task.jetbrains_internship_test_task.server.service;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.repository.QueryExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryExecutionServiceTest {

    @Mock
    private QueryExecutionRepository queryExecutionRepository;

    @Mock
    private StoredQueryService storedQueryService;

    @InjectMocks
    private QueryExecutionService queryExecutionService;

    @Test
    void executeQuery_ValidQuery_ReturnsFormattedResults() {
        String query = "SELECT name, age FROM users";
        List<Map<String, Object>> mockResult = List.of(
                Map.of("name", "John Doe", "age", 30),
                Map.of("name", "Jane Smith", "age", 25)
        );

        when(queryExecutionRepository.executeNativeQuery(query)).thenReturn(mockResult);
        
        List<List<Object>> result = queryExecutionService.executeQuery(query);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        assertTrue(result.getFirst().contains("John Doe"));
        assertTrue(result.getFirst().contains(30));
        
        assertTrue(result.get(1).contains("Jane Smith"));
        assertTrue(result.get(1).contains(25));

        verify(queryExecutionRepository).executeNativeQuery(query);
    }

    @Test
    void executeQuery_EmptyResult_ReturnsEmptyList() {
        String query = "SELECT * FROM empty_table";
        List<Map<String, Object>> mockResult = List.of();

        when(queryExecutionRepository.executeNativeQuery(query)).thenReturn(mockResult);

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(queryExecutionRepository).executeNativeQuery(query);
    }

    @Test
    void executeQuery_NullValuesInResult_HandlesNullsCorrectly() {
        String query = "SELECT name, nullable_column FROM users";

        List<Map<String, Object>> mockResult = Arrays.asList(
                new HashMap<String, Object>() {{
                    put("name", "John Doe");
                    put("nullable_column", null);
                }},
                new HashMap<String, Object>() {{
                    put("name", "Jane Smith");
                    put("nullable_column", "value");
                }}
        );

        when(queryExecutionRepository.executeNativeQuery(query)).thenReturn(mockResult);

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.getFirst().contains(null));
        assertTrue(result.get(1).contains("value"));
    }

    @Test
    void executeQueryById_ValidId_ReturnsQueryResults() {
        Long queryId = 1L;
        String queryText = "SELECT * FROM users WHERE active = true";
        StoredQuery storedQuery = new StoredQuery();
        storedQuery.setId(queryId);
        storedQuery.setQuery(queryText);

        List<Map<String, Object>> mockResult = List.of(
                Map.of("id", 1, "name", "Active User")
        );

        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(queryExecutionRepository.executeNativeQuery(queryText)).thenReturn(mockResult);

        
        List<List<Object>> result = queryExecutionService.executeQueryById(queryId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().contains(1));
        assertTrue(result.getFirst().contains("Active User"));

        verify(storedQueryService).getQueryById(queryId);
        verify(queryExecutionRepository).executeNativeQuery(queryText);
    }

    @Test
    void executeQueryById_NonExistentId_ReturnsNull() {
        Long nonExistentId = 999L;

        when(storedQueryService.getQueryById(nonExistentId)).thenReturn(Optional.empty());
        
        List<List<Object>> result = queryExecutionService.executeQueryById(nonExistentId);
        
        assertNull(result);
        verify(storedQueryService).getQueryById(nonExistentId);
        verify(queryExecutionRepository, never()).executeNativeQuery(anyString());
    }

    @Test
    void executeQueryById_EmptyStoredQuery_ReturnsEmptyResults() {
        Long queryId = 2L;
        String queryText = "SELECT * FROM empty_table";
        StoredQuery storedQuery = new StoredQuery();
        storedQuery.setId(queryId);
        storedQuery.setQuery(queryText);

        List<Map<String, Object>> mockResult = List.of();

        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(queryExecutionRepository.executeNativeQuery(queryText)).thenReturn(mockResult);
        
        List<List<Object>> result = queryExecutionService.executeQueryById(queryId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(storedQueryService).getQueryById(queryId);
        verify(queryExecutionRepository).executeNativeQuery(queryText);
    }

    @Test
    void convertResultToList_MultipleDataTypes_ConvertsCorrectly() {
        List<Map<String, Object>> complexData = List.of(
                new HashMap<String, Object>() {{
                    put("string_col", "text");
                    put("int_col", 123);
                    put("bool_col", true);
                    put("double_col", 45.67);
                    put("null_col", null);
                }}
        );

        String dummyQuery = "SELECT * FROM complex_table";
        when(queryExecutionRepository.executeNativeQuery(dummyQuery)).thenReturn(complexData);

        List<List<Object>> result = queryExecutionService.executeQuery(dummyQuery);

        assertNotNull(result);
        assertEquals(1, result.size());

        List<Object> row = result.getFirst();
        assertEquals(5, row.size());
        assertTrue(row.contains("text"));
        assertTrue(row.contains(123));
        assertTrue(row.contains(true));
        assertTrue(row.contains(45.67));
        assertTrue(row.contains(null));
    }

    @Test
    void executeQuery_SingleColumnResult_ConvertsCorrectly() {
        String query = "SELECT COUNT(*) as count FROM users";
        List<Map<String, Object>> mockResult = List.of(
                Map.of("count", 150L)
        );

        when(queryExecutionRepository.executeNativeQuery(query)).thenReturn(mockResult);

        
        List<List<Object>> result = queryExecutionService.executeQuery(query);

        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().size());
        assertEquals(150L, result.getFirst().getFirst());
    }

    @Test
    void executeQuery_RepositoryThrowsException_PropagatesException() {
        String invalidQuery = "INVALID SQL SYNTAX";

        when(queryExecutionRepository.executeNativeQuery(invalidQuery))
                .thenThrow(new RuntimeException("SQL syntax error"));

        assertThrows(RuntimeException.class, () -> {
            queryExecutionService.executeQuery(invalidQuery);
        });

        verify(queryExecutionRepository).executeNativeQuery(invalidQuery);
    }

    @Test
    void executeQuery_LargeResultSet_HandlesCorrectly() {
        String query = "SELECT * FROM large_table";
        List<Map<String, Object>> largeResult = createLargeResultSet(1000);

        when(queryExecutionRepository.executeNativeQuery(query)).thenReturn(largeResult);

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertEquals(1000, result.size());
        verify(queryExecutionRepository).executeNativeQuery(query);
    }

    @Test
    void executeQuery_SpecialCharactersInData_HandlesCorrectly() {
        String query = "SELECT text FROM special_chars";
        List<Map<String, Object>> mockResult = List.of(
                Map.of("text", "Line1\nLine2"),
                Map.of("text", "Tab\tSeparated"),
                Map.of("text", "Unicode: é"),
                Map.of("text", "Quote: \"test\"")
        );

        when(queryExecutionRepository.executeNativeQuery(query)).thenReturn(mockResult);

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.get(0).contains("Line1\nLine2"));
        assertTrue(result.get(1).contains("Tab\tSeparated"));
    }

    private List<Map<String, Object>> createLargeResultSet(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> Map.<String, Object>of("id", i, "value", "row_" + i))
                .toList();
    }
}