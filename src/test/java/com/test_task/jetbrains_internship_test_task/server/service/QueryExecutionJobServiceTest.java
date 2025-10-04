package com.test_task.jetbrains_internship_test_task.server.service;

import com.test_task.jetbrains_internship_test_task.entity.QueryExecutionJob;
import com.test_task.jetbrains_internship_test_task.server.repository.QueryExecutionJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryExecutionJobServiceTest {

    @Mock
    private QueryExecutionJobRepository jobRepository;

    @InjectMocks
    private QueryExecutionJobService jobService;

    @Test
    void addJob_ValidQueryId_ReturnsSavedJob() {
        Long queryId = 1L;
        QueryExecutionJob savedJob = new QueryExecutionJob();
        savedJob.setId(100L);
        savedJob.setSourceQueryId(queryId);
        savedJob.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.save(any(QueryExecutionJob.class))).thenReturn(savedJob);

        QueryExecutionJob result = jobService.addJob(queryId);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(queryId, result.getSourceQueryId());
        assertEquals(QueryExecutionJob.JobStatus.PENDING, result.getStatus());

        verify(jobRepository).save(any(QueryExecutionJob.class));
    }

    @Test
    void addJob_NullQueryId_ThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> jobService.addJob(null));

        assertEquals("Query cannot be null", exception.getMessage());
        verifyNoInteractions(jobRepository);
    }

    @Test
    void addJob_ZeroQueryId_ReturnsJob() {
        Long queryId = 0L;
        QueryExecutionJob savedJob = new QueryExecutionJob();
        savedJob.setId(1L);
        savedJob.setSourceQueryId(queryId);
        savedJob.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.save(any(QueryExecutionJob.class))).thenReturn(savedJob);

        QueryExecutionJob result = jobService.addJob(queryId);

        assertNotNull(result);
        assertEquals(queryId, result.getSourceQueryId());
        verify(jobRepository).save(any(QueryExecutionJob.class));
    }

    @Test
    void addJob_NegativeQueryId_ReturnsJob() {
        Long queryId = -1L;
        QueryExecutionJob savedJob = new QueryExecutionJob();
        savedJob.setId(1L);
        savedJob.setSourceQueryId(queryId);
        savedJob.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.save(any(QueryExecutionJob.class))).thenReturn(savedJob);

        QueryExecutionJob result = jobService.addJob(queryId);

        assertNotNull(result);
        assertEquals(queryId, result.getSourceQueryId());
        verify(jobRepository).save(any(QueryExecutionJob.class));
    }

    @Test
    void addJob_JobCreation_SetsCorrectInitialStatus() {
        Long queryId = 5L;
        QueryExecutionJob savedJob = new QueryExecutionJob();
        savedJob.setId(1L);
        savedJob.setSourceQueryId(queryId);
        savedJob.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.save(any(QueryExecutionJob.class))).thenReturn(savedJob);

        QueryExecutionJob result = jobService.addJob(queryId);

        assertEquals(QueryExecutionJob.JobStatus.PENDING, result.getStatus());
        assertNull(result.getResult());
        assertNull(result.getErrorMessage());
        verify(jobRepository).save(any(QueryExecutionJob.class));
    }

    @Test
    void getAllJobs_EmptyList_ReturnsEmptyList() {
        when(jobRepository.findAllByOrderByIdDesc()).thenReturn(List.of());

        List<QueryExecutionJob> result = jobService.getAllJobs();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jobRepository).findAllByOrderByIdDesc();
    }

    @Test
    void getAllJobs_WithJobs_ReturnsJobsOrderedByIdDesc() {
        QueryExecutionJob job1 = new QueryExecutionJob();
        job1.setId(3L);
        job1.setSourceQueryId(1L);
        job1.setStatus(QueryExecutionJob.JobStatus.COMPLETED);

        QueryExecutionJob job2 = new QueryExecutionJob();
        job2.setId(2L);
        job2.setSourceQueryId(2L);
        job2.setStatus(QueryExecutionJob.JobStatus.RUNNING);

        QueryExecutionJob job3 = new QueryExecutionJob();
        job3.setId(1L);
        job3.setSourceQueryId(3L);
        job3.setStatus(QueryExecutionJob.JobStatus.PENDING);

        List<QueryExecutionJob> jobs = List.of(job1, job2, job3);

        when(jobRepository.findAllByOrderByIdDesc()).thenReturn(jobs);

        List<QueryExecutionJob> result = jobService.getAllJobs();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(1L, result.get(2).getId());

        verify(jobRepository).findAllByOrderByIdDesc();
    }

    @Test
    void getAllJobs_SingleJob_ReturnsSingleJobList() {
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(1L);
        job.setSourceQueryId(1L);
        job.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.findAllByOrderByIdDesc()).thenReturn(List.of(job));

        List<QueryExecutionJob> result = jobService.getAllJobs();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(job, result.getFirst());
        verify(jobRepository).findAllByOrderByIdDesc();
    }

    @Test
    void getJobById_ExistingJob_ReturnsJob() {
        Long jobId = 1L;
        QueryExecutionJob job = new QueryExecutionJob();
        job.setId(jobId);
        job.setSourceQueryId(5L);
        job.setStatus(QueryExecutionJob.JobStatus.COMPLETED);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Optional<QueryExecutionJob> result = jobService.getJobById(jobId);

        assertTrue(result.isPresent());
        assertEquals(job, result.get());
        assertEquals(jobId, result.get().getId());
        verify(jobRepository).findById(jobId);
    }

    @Test
    void getJobById_NonExistentJob_ReturnsEmptyOptional() {
        Long jobId = 999L;
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        Optional<QueryExecutionJob> result = jobService.getJobById(jobId);

        assertTrue(result.isEmpty());
        verify(jobRepository).findById(jobId);
    }

    @Test
    void getJobById_NullId_ReturnsEmptyOptional() {
        when(jobRepository.findById(null)).thenReturn(Optional.empty());

        Optional<QueryExecutionJob> result = jobService.getJobById(null);

        assertTrue(result.isEmpty());
        verify(jobRepository).findById(null);
    }

    @Test
    void getJobById_ZeroId_ReturnsEmptyOptional() {
        Long jobId = 0L;
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        Optional<QueryExecutionJob> result = jobService.getJobById(jobId);

        assertTrue(result.isEmpty());
        verify(jobRepository).findById(jobId);
    }

    @Test
    void getJobById_NegativeId_ReturnsEmptyOptional() {
        Long jobId = -1L;
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        Optional<QueryExecutionJob> result = jobService.getJobById(jobId);

        assertTrue(result.isEmpty());
        verify(jobRepository).findById(jobId);
    }


    @Test
    void addJob_MultipleCalls_CreatesDistinctJobs() {
        Long queryId1 = 1L;
        Long queryId2 = 2L;

        QueryExecutionJob job1 = new QueryExecutionJob();
        job1.setId(1L);
        job1.setSourceQueryId(queryId1);
        job1.setStatus(QueryExecutionJob.JobStatus.PENDING);

        QueryExecutionJob job2 = new QueryExecutionJob();
        job2.setId(2L);
        job2.setSourceQueryId(queryId2);
        job2.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.save(any(QueryExecutionJob.class)))
                .thenReturn(job1)
                .thenReturn(job2);

        QueryExecutionJob result1 = jobService.addJob(queryId1);
        QueryExecutionJob result2 = jobService.addJob(queryId2);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(queryId1, result1.getSourceQueryId());
        assertEquals(queryId2, result2.getSourceQueryId());
        assertNotEquals(result1.getId(), result2.getId());

        verify(jobRepository, times(2)).save(any(QueryExecutionJob.class));
    }

    @Test
    void addJob_SameQueryIdMultipleTimes_CreatesMultipleJobs() {
        Long queryId = 1L;

        QueryExecutionJob job1 = new QueryExecutionJob();
        job1.setId(1L);
        job1.setSourceQueryId(queryId);
        job1.setStatus(QueryExecutionJob.JobStatus.PENDING);

        QueryExecutionJob job2 = new QueryExecutionJob();
        job2.setId(2L);
        job2.setSourceQueryId(queryId);
        job2.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.save(any(QueryExecutionJob.class)))
                .thenReturn(job1)
                .thenReturn(job2);

        QueryExecutionJob result1 = jobService.addJob(queryId);
        QueryExecutionJob result2 = jobService.addJob(queryId);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(queryId, result1.getSourceQueryId());
        assertEquals(queryId, result2.getSourceQueryId());
        // Different job IDs for same query ID
        assertNotEquals(result1.getId(), result2.getId());

        verify(jobRepository, times(2)).save(any(QueryExecutionJob.class));
    }

    @Test
    void jobProperties_AfterCreation_HaveCorrectDefaults() {
        Long queryId = 1L;
        QueryExecutionJob savedJob = new QueryExecutionJob();
        savedJob.setId(100L);
        savedJob.setSourceQueryId(queryId);
        savedJob.setStatus(QueryExecutionJob.JobStatus.PENDING);

        when(jobRepository.save(any(QueryExecutionJob.class))).thenReturn(savedJob);

        QueryExecutionJob result = jobService.addJob(queryId);

        assertNotNull(result);
        assertEquals(QueryExecutionJob.JobStatus.PENDING, result.getStatus());
        assertNull(result.getResult());
        assertNull(result.getErrorMessage());
        verify(jobRepository).save(any(QueryExecutionJob.class));
    }
}