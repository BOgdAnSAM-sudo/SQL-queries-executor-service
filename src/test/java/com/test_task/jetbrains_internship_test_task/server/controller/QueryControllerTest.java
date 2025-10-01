package com.test_task.jetbrains_internship_test_task.server.controller;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.service.QueryExecutionService;
import com.test_task.jetbrains_internship_test_task.server.service.StoredQueryService;
import com.test_task.jetbrains_internship_test_task.server.service.StoredQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryControllerTest {

    @Mock
    private StoredQueryService queryService;

    @Mock
    private QueryExecutionService executionService;

    @InjectMocks
    private QueryController queryController;

    @Test
    public void storeQuery_ValidQuery_ReturnsId() {
        String queryText = "SELECT * FROM passengers";
        StoredQuery storedQuery = new StoredQuery(queryText);
        storedQuery.setId(1L);

        when(queryService.addQuery(queryText)).thenReturn(storedQuery);

        ResponseEntity<Map<String, Long>> response = queryController.storeQuery(queryText);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().get("id"));
        assertNull(response.getHeaders().getContentType());

        verify(queryService).addQuery(queryText);
    }

    @Test
    public void storeQuery_ComplexQuery_ReturnsId() {
        String complexQuery = "SELECT name, age FROM passengers WHERE age > 18 AND survived = 1 ORDER BY name";
        StoredQuery storedQuery = new StoredQuery(complexQuery);
        storedQuery.setId(2L);

        when(queryService.addQuery(complexQuery)).thenReturn(storedQuery);

        ResponseEntity<Map<String, Long>> response = queryController.storeQuery(complexQuery);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2L, Objects.requireNonNull(response.getBody()).get("id"));
        verify(queryService).addQuery(complexQuery);
    }

    @Test
    public void storeQuery_ServiceThrowsException_PropagatesException() {
        String queryText = "SELECT * FROM table";
        when(queryService.addQuery(queryText))
                .thenThrow(new StoredQueryException("Invalid query"));

        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryController.storeQuery(queryText));

        assertEquals("Invalid query", exception.getMessage());
        verify(queryService).addQuery(queryText);
    }

    @Test
    public void storeQuery_NullQuery_ThrowsException() {
        when(queryService.addQuery(null))
                .thenThrow(new StoredQueryException("Query cannot be null"));

        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryController.storeQuery(null));

        assertEquals("Query cannot be null", exception.getMessage());
    }

    @Test
    public void storeQuery_EmptyQuery_ThrowsException() {
        when(queryService.addQuery(""))
                .thenThrow(new StoredQueryException("Query cannot be empty"));

        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryController.storeQuery(""));

        assertEquals("Query cannot be empty", exception.getMessage());
    }

    @Test
    public void getAllQueries_EmptyList_ReturnsEmptyList() {
        when(queryService.getAllQueries()).thenReturn(List.of());

        ResponseEntity<List<StoredQuery>> response = queryController.getAllQueries();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        assertNull(response.getHeaders().getContentType());

        verify(queryService).getAllQueries();
    }

    @Test
    public void getAllQueries_WithQueries_ReturnsList() {
        StoredQuery query1 = new StoredQuery("SELECT * FROM table1");
        query1.setId(1L);
        query1.setCreatedAt(LocalDateTime.now());

        StoredQuery query2 = new StoredQuery("SELECT name FROM table2");
        query2.setId(2L);
        query2.setCreatedAt(LocalDateTime.now().minusHours(1));

        List<StoredQuery> queries = List.of(query1, query2);

        when(queryService.getAllQueries()).thenReturn(queries);

        ResponseEntity<List<StoredQuery>> response = queryController.getAllQueries();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        assertEquals(query1, response.getBody().get(0));
        assertEquals(query2, response.getBody().get(1));

        verify(queryService).getAllQueries();
    }

    @Test
    public void getAllQueries_ServiceThrowsException_PropagatesException() {
        when(queryService.getAllQueries())
                .thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> queryController.getAllQueries());

        assertEquals("Database error", exception.getMessage());
        verify(queryService).getAllQueries();
    }

    @Test
    public void executeQuery_ValidId_ReturnsResults() {
        Long queryId = 1L;
        List<List<Object>> mockResults = List.of(
                List.of("John Doe", 30, "Engineer"),
                List.of("Jane Smith", 25, "Designer")
        );

        when(executionService.executeQueryById(queryId)).thenReturn(mockResults);

        ResponseEntity<List<List<Object>>> response = queryController.executeQuery(queryId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(3, response.getBody().getFirst().size());
        assertEquals("John Doe", response.getBody().getFirst().getFirst());
        assertNull(response.getHeaders().getContentType());

        verify(executionService).executeQueryById(queryId);
    }

    @Test
    public void executeQuery_EmptyResults_ReturnsEmptyList() {
        Long queryId = 2L;
        List<List<Object>> emptyResults = List.of();

        when(executionService.executeQueryById(queryId)).thenReturn(emptyResults);

        ResponseEntity<List<List<Object>>> response = queryController.executeQuery(queryId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(executionService).executeQueryById(queryId);
    }

    @Test
    public void executeQuery_NullResults_ReturnsNull() {
        Long queryId = 999L;
        when(executionService.executeQueryById(queryId)).thenReturn(null);

        ResponseEntity<List<List<Object>>> response = queryController.executeQuery(queryId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(executionService).executeQueryById(queryId);
    }

    @Test
    public void executeQuery_NonExistentId_ReturnsNull() {
        Long nonExistentId = 999L;
        when(executionService.executeQueryById(nonExistentId)).thenReturn(null);

        ResponseEntity<List<List<Object>>> response = queryController.executeQuery(nonExistentId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(executionService).executeQueryById(nonExistentId);
    }

    @Test
    public void executeQuery_ExecutionThrowsException_PropagatesException() {
        Long queryId = 1L;
        when(executionService.executeQueryById(queryId))
                .thenThrow(new RuntimeException("SQL execution error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> queryController.executeQuery(queryId));

        assertEquals("SQL execution error", exception.getMessage());
        verify(executionService).executeQueryById(queryId);
    }

    @Test
    public void executeQuery_InvalidQueryId_ServiceHandlesGracefully() {
        Long invalidId = -1L;
        when(executionService.executeQueryById(invalidId)).thenReturn(null);

        ResponseEntity<List<List<Object>>> response = queryController.executeQuery(invalidId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(executionService).executeQueryById(invalidId);
    }


    @Test
    public void executeQuery_ComplexDataTypes_ReturnsCorrectly() {
        Long queryId = 5L;
        List<List<Object>> complexResults = Arrays.asList(
                Arrays.asList("String value", 123, 45.67, true, null),
                Arrays.asList("Another string", 456, 78.90, false, "not null")
        );

        when(executionService.executeQueryById(queryId)).thenReturn(complexResults);

        ResponseEntity<List<List<Object>>> response = queryController.executeQuery(queryId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());

        List<Object> firstRow = response.getBody().getFirst();
        assertEquals(5, firstRow.size());
        assertEquals("String value", firstRow.get(0));
        assertEquals(123, firstRow.get(1));
        assertEquals(45.67, firstRow.get(2));
        assertEquals(true, firstRow.get(3));
        assertNull(firstRow.get(4));

        verify(executionService).executeQueryById(queryId);
    }

    @Test
    public void storeQuery_ResponseStructure_ContainsOnlyId() {
        String queryText = "SELECT * FROM test";
        StoredQuery storedQuery = new StoredQuery(queryText);
        storedQuery.setId(42L);

        when(queryService.addQuery(queryText)).thenReturn(storedQuery);

        ResponseEntity<Map<String, Long>> response = queryController.storeQuery(queryText);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Long> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertTrue(body.containsKey("id"));
        assertEquals(42L, body.get("id"));
        assertFalse(body.containsKey("query"));
        assertFalse(body.containsKey("createdAt"));
    }

}