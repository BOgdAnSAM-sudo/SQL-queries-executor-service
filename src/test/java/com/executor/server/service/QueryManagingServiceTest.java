package com.executor.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.executor.entity.QueryExecutionJob;
import com.executor.entity.StoredQuery;
import com.executor.server.repository.QueryExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryManagingServiceTest {

    @Mock
    QueryExecutionService queryExecutionService;

    @Mock
    private StoredQueryService storedQueryService;

    @Mock
    private QueryExecutionJobService jobService;

    @InjectMocks
    private QueryManagingService QueryManagingService;

    @Test
    void executeQuery_ValidJobId_ExecutesSuccessfully() throws Exception {
        Long jobId = 1L;
        Long queryId = 1L;
        String queryText = "SELECT name, age FROM users";

        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setSourceQueryId(queryId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        StoredQuery storedQuery = new StoredQuery();
        storedQuery.setId(queryId);
        storedQuery.setQuery(queryText);

        List<Map<String, Object>> mockResult = List.of(
                Map.of("name", "John Doe", "age", 30),
                Map.of("name", "Jane Smith", "age", 25)
        );

        String resultJson = "[[\"John Doe\",30],[\"Jane Smith\",25]]";

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));
        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(queryExecutionService.cacheableQueryExecution(queryText)).thenReturn(resultJson);

        QueryManagingService.executeQuery(jobId);

        verify(jobService).getJobById(jobId);
        verify(storedQueryService).getQueryById(queryId);
        verify(queryExecutionService).cacheableQueryExecution(queryText);

        verify(jobService).markJobCompleted(eq(jobId), eq(resultJson));
        assertNull(job.getErrorMessage());
    }

    @Test
    void executeQuery_JobNotFound_ThrowsException() {
        Long jobId = 999L;

        when(jobService.getJobById(jobId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> QueryManagingService.executeQuery(jobId));

        assertTrue(exception.getMessage().contains("No value present"));
        verify(jobService).getJobById(jobId);
        verifyNoInteractions(storedQueryService, queryExecutionService);
    }

    @Test
    void executeQuery_StoredQueryNotFound_MarksJobAsFailed() {
        Long jobId = 1L;
        Long queryId = 999L;

        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setSourceQueryId(queryId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));
        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.empty());

        QueryManagingService.executeQuery(jobId);

        verify(jobService).getJobById(jobId);
        verify(storedQueryService).getQueryById(queryId);
        verifyNoInteractions(queryExecutionService, queryExecutionService);

        verify(jobService).markJobFailed(eq(jobId), anyString());
    }

    @Test
    void executeQuery_QueryExecutionFails_MarksJobAsFailed() throws JsonProcessingException {
        Long jobId = 1L;
        Long queryId = 1L;
        String queryText = "SELECT * FROM users";

        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setSourceQueryId(queryId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        StoredQuery storedQuery = new StoredQuery();
        storedQuery.setId(queryId);
        storedQuery.setQuery(queryText);

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));
        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(queryExecutionService.cacheableQueryExecution(queryText))
                .thenThrow(new RuntimeException("Database connection failed"));

        QueryManagingService.executeQuery(jobId);

        verify(jobService).getJobById(jobId);
        verify(storedQueryService).getQueryById(queryId);
        verify(queryExecutionService).cacheableQueryExecution(queryText);

        verify(jobService).markJobFailed(eq(jobId), anyString());
    }

    @Test
    void executeQuery_WithNullValuesInResult_HandlesCorrectly() throws Exception {
        Long jobId = 1L;
        Long queryId = 1L;
        String queryText = "SELECT name, nullable_col FROM users";

        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setSourceQueryId(queryId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        StoredQuery storedQuery = new StoredQuery();
        storedQuery.setId(queryId);
        storedQuery.setQuery(queryText);

        List<Map<String, Object>> mockResult = List.of(
                new HashMap<>() {{
                    put("name", "John Doe");
                    put("nullable_col", null);
                }},
                new HashMap<>() {{
                    put("name", "Jane Smith");
                    put("nullable_col", "value");
                }}
        );

        String resultJson = "[[\"John Doe\",null],[\"Jane Smith\",\"value\"]]";

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));
        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(queryExecutionService.cacheableQueryExecution(queryText)).thenReturn(resultJson);

        QueryManagingService.executeQuery(jobId);

        verify(jobService).markJobCompleted(eq(jobId), eq(resultJson));
    }
}