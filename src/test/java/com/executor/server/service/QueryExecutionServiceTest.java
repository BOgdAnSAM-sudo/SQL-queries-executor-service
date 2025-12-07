package com.executor.server.service;

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
class QueryExecutionServiceTest {

    @Mock
    private QueryExecutionRepository queryExecutionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StoredQueryService storedQueryService;

    @Mock
    private QueryExecutionJobService jobService;

    @InjectMocks
    private QueryExecutionService QueryExecutionService;

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
        when(queryExecutionRepository.executeNativeQuery(queryText)).thenReturn(mockResult);
        when(objectMapper.writeValueAsString(any())).thenReturn(resultJson);

        QueryExecutionService.executeQuery(jobId);

        verify(jobService).getJobById(jobId);
        verify(storedQueryService).getQueryById(queryId);
        verify(queryExecutionRepository).executeNativeQuery(queryText);
        verify(objectMapper).writeValueAsString(any());

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, job.getStatus());
        assertEquals(resultJson, job.getResult());
        assertNull(job.getErrorMessage());
    }

    @Test
    void executeQuery_JobNotFound_ThrowsException() {
        Long jobId = 999L;

        when(jobService.getJobById(jobId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> QueryExecutionService.executeQuery(jobId));

        assertTrue(exception.getMessage().contains("No value present"));
        verify(jobService).getJobById(jobId);
        verifyNoInteractions(storedQueryService, queryExecutionRepository, objectMapper);
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

        assertThrows(RuntimeException.class, () -> QueryExecutionService.executeQuery(jobId));

        verify(jobService).getJobById(jobId);
        verify(storedQueryService).getQueryById(queryId);
        verifyNoInteractions(queryExecutionRepository, objectMapper);

        assertEquals(QueryExecutionJob.JobStatus.FAILED, job.getStatus());
        assertNotNull(job.getErrorMessage());
        assertTrue(job.getErrorMessage().contains("Source query not found"));
    }

    @Test
    void executeQuery_QueryExecutionFails_MarksJobAsFailed() {
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
        when(queryExecutionRepository.executeNativeQuery(queryText))
                .thenThrow(new RuntimeException("Database connection failed"));

        QueryExecutionService.executeQuery(jobId);

        verify(jobService).getJobById(jobId);
        verify(storedQueryService).getQueryById(queryId);
        verify(queryExecutionRepository).executeNativeQuery(queryText);
        verifyNoInteractions(objectMapper);

        assertEquals(QueryExecutionJob.JobStatus.FAILED, job.getStatus());
        assertNotNull(job.getErrorMessage());
        assertEquals("Database connection failed", job.getErrorMessage());
    }

    @Test
    void executeQuery_JsonSerializationFails_MarksJobAsFailed() throws Exception {
        Long jobId = 1L;
        Long queryId = 1L;
        String queryText = "SELECT name FROM users";

        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setSourceQueryId(queryId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        StoredQuery storedQuery = new StoredQuery();
        storedQuery.setId(queryId);
        storedQuery.setQuery(queryText);

        List<Map<String, Object>> mockResult = List.of(
                Map.of("name", "John Doe")
        );

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));
        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(queryExecutionRepository.executeNativeQuery(queryText)).thenReturn(mockResult);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("JSON serialization failed"));

        QueryExecutionService.executeQuery(jobId);

        verify(jobService).getJobById(jobId);
        verify(storedQueryService).getQueryById(queryId);
        verify(queryExecutionRepository).executeNativeQuery(queryText);
        verify(objectMapper).writeValueAsString(any());

        assertEquals(QueryExecutionJob.JobStatus.FAILED, job.getStatus());
        assertNotNull(job.getErrorMessage());
        assertEquals("JSON serialization failed", job.getErrorMessage());
    }

    @Test
    void executeQuery_EmptyResult_SavesEmptyJsonArray() throws Exception {
        Long jobId = 1L;
        Long queryId = 1L;
        String queryText = "SELECT * FROM empty_table";

        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setSourceQueryId(queryId);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        StoredQuery storedQuery = new StoredQuery();
        storedQuery.setId(queryId);
        storedQuery.setQuery(queryText);

        List<Map<String, Object>> mockResult = List.of();
        String emptyJsonArray = "[]";

        when(jobService.getJobById(jobId)).thenReturn(Optional.of(job));
        when(storedQueryService.getQueryById(queryId)).thenReturn(Optional.of(storedQuery));
        when(queryExecutionRepository.executeNativeQuery(queryText)).thenReturn(mockResult);
        when(objectMapper.writeValueAsString(any())).thenReturn(emptyJsonArray);

        QueryExecutionService.executeQuery(jobId);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, job.getStatus());
        assertEquals(emptyJsonArray, job.getResult());
        assertNull(job.getErrorMessage());
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
        when(queryExecutionRepository.executeNativeQuery(queryText)).thenReturn(mockResult);
        when(objectMapper.writeValueAsString(any())).thenReturn(resultJson);

        QueryExecutionService.executeQuery(jobId);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, job.getStatus());
        assertEquals(resultJson, job.getResult());
        assertNull(job.getErrorMessage());
    }
}