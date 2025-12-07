package com.executor.server.controller;

import com.executor.entity.QueryExecutionJob;
import com.executor.entity.StoredQuery;
import com.executor.server.service.QueryExecutionJobService;
import com.executor.server.service.AsyncQueryExecutionService;
import com.executor.server.service.StoredQueryService;
import com.executor.server.service.StoredQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryControllerTest {

    @Mock
    private StoredQueryService queryService;

    @Mock
    private AsyncQueryExecutionService executionService;

    @Mock
    private QueryExecutionJobService jobService;

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
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().get("id"));

        verify(queryService).addQuery(queryText);
    }

    @Test
    public void storeQuery_ComplexQuery_ReturnsId() {
        String complexQuery = "SELECT name, age FROM passengers WHERE age > 18 AND survived = 1 ORDER BY name";
        StoredQuery storedQuery = new StoredQuery(complexQuery);
        storedQuery.setId(2L);

        when(queryService.addQuery(complexQuery)).thenReturn(storedQuery);

        ResponseEntity<Map<String, Long>> response = queryController.storeQuery(complexQuery);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
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
    public void getAllQueries_EmptyList_ReturnsEmptyList() {
        when(queryService.getAllQueries()).thenReturn(List.of());

        ResponseEntity<List<StoredQuery>> response = queryController.getAllQueries();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
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
    public void executeQuery_ValidQueryId_StartsJobExecution() {
        Long queryId = 1L;
        StoredQuery storedQuery = new StoredQuery("SELECT * FROM test");
        storedQuery.setId(queryId);

        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(100L);
        job.setSourceQueryId(queryId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(queryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(jobService.getJobById(queryId)).thenReturn(Optional.empty());
        when(jobService.addJob(queryId)).thenReturn(job);
        doNothing().when(executionService).executeQuery(job.getId());

        ResponseEntity<?> response = queryController.executeQuery(queryId);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("100", responseBody.get("jobId"));
        assertEquals("PENDING", responseBody.get("status"));
        assertEquals("Query execution started. Check status endpoint for progress.", responseBody.get("message"));

        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains("/execution/100/status"));

        verify(queryService).getQueryById(queryId);
        verify(jobService).getJobById(queryId);
        verify(jobService).addJob(queryId);
        verify(executionService).executeQuery(job.getId());
    }

    @Test
    public void executeQuery_QueryNotFound_ThrowsException() {
        Long queryId = 999L;
        when(queryService.getQueryById(queryId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> queryController.executeQuery(queryId));

        assertEquals("Query not found", exception.getMessage());
        verify(queryService).getQueryById(queryId);
        verifyNoInteractions(jobService, executionService);
    }

    @Test
    public void executeQuery_JobAlreadyExists_ThrowsException() {
        Long queryId = 1L;
        StoredQuery storedQuery = new StoredQuery("SELECT * FROM test");
        storedQuery.setId(queryId);

        QueryExecutionJob existingJob = new QueryExecutionJob();
        existingJob.setId(queryId);

        when(queryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(jobService.getJobById(queryId)).thenReturn(Optional.of(existingJob));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> queryController.executeQuery(queryId));

        assertEquals("Job already exists", exception.getMessage());
        verify(queryService).getQueryById(queryId);
        verify(jobService).getJobById(queryId);
        verifyNoMoreInteractions(jobService);
        verifyNoInteractions(executionService);
    }

    @Test
    public void getStatus_ValidJobId_ReturnsStatus() {
        Long jobId = 100L;
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setStatus(QueryExecutionJob.JobStatus.RUNNING);

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = queryController.getStatus(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(jobId, responseBody.get("jobId"));
        assertEquals(QueryExecutionJob.JobStatus.RUNNING, responseBody.get("status"));

        verify(jobService).getJobById(jobId);
    }

    @Test
    public void getStatus_JobNotFound_ThrowsException() {
        Long jobId = 999L;
        when(jobService.getJobById(jobId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> queryController.getStatus(jobId));

        assertEquals("Job not found", exception.getMessage());
        verify(jobService).getJobById(jobId);
    }

    @Test
    public void getStatus_CompletedJob_ReturnsCompletedStatus() {
        Long jobId = 100L;
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setStatus(QueryExecutionJob.JobStatus.COMPLETED);

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = queryController.getStatus(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, responseBody.get("status"));
    }

    @Test
    public void getResult_CompletedJob_ReturnsResult() {
        Long jobId = 100L;
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setStatus(QueryExecutionJob.JobStatus.COMPLETED);
        job.setResult("[[\"John\",30],[\"Jane\",25]]");

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = queryController.getResult(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(jobId, responseBody.get("jobId"));
        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, responseBody.get("status"));
        assertEquals("[[\"John\",30],[\"Jane\",25]]", responseBody.get("result"));

        verify(jobService).getJobById(jobId);
    }

    @Test
    public void getResult_PendingJob_ReturnsStatusMessage() {
        Long jobId = 100L;
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = queryController.getResult(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(QueryExecutionJob.JobStatus.PENDING, responseBody.get("status"));
        assertEquals("Result not yet available.", responseBody.get("message"));
        assertNull(responseBody.get("result"));
    }

    @Test
    public void getResult_RunningJob_ReturnsStatusMessage() {
        Long jobId = 100L;
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setStatus(QueryExecutionJob.JobStatus.RUNNING);

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = queryController.getResult(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(QueryExecutionJob.JobStatus.RUNNING, responseBody.get("status"));
        assertEquals("Result not yet available.", responseBody.get("message"));
    }

    @Test
    public void getResult_FailedJob_ReturnsStatusWithoutResult() {
        Long jobId = 100L;
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setStatus(QueryExecutionJob.JobStatus.FAILED);
        job.setErrorMessage("Query execution failed");

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = queryController.getResult(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals(QueryExecutionJob.JobStatus.FAILED, responseBody.get("status"));
        assertEquals("Result not yet available.", responseBody.get("message"));
        assertNull(responseBody.get("result"));
    }

    @Test
    public void getResult_JobNotFound_ThrowsException() {
        Long jobId = 999L;
        when(jobService.getJobById(jobId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> queryController.getResult(jobId));

        assertEquals("Job not found", exception.getMessage());
        verify(jobService).getJobById(jobId);
    }

    @Test
    public void storeQuery_ResponseStructure_ContainsOnlyId() {
        String queryText = "SELECT * FROM test";
        StoredQuery storedQuery = new StoredQuery(queryText);
        storedQuery.setId(42L);

        when(queryService.addQuery(queryText)).thenReturn(storedQuery);

        ResponseEntity<Map<String, Long>> response = queryController.storeQuery(queryText);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, Long> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertTrue(body.containsKey("id"));
        assertEquals(42L, body.get("id"));
        assertFalse(body.containsKey("query"));
        assertFalse(body.containsKey("createdAt"));
    }
}