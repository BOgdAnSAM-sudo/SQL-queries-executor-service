package com.executor.server.service;

import com.executor.server.repository.QueryExecutionJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryExecutionJobCleanupServiceTest {

    @Mock
    private QueryExecutionJobRepository repository;

    @InjectMocks
    private QueryExecutionJobCleanupService service;

    @Test
    void cleanupOldJobs_shouldDeleteJobsOlderThanRetentionPeriod() {
        when(repository.deleteOlderThan(any(LocalDateTime.class))).thenReturn(5);

        service.cleanupOldJobs();

        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository, times(1)).deleteOlderThan(dateCaptor.capture());

        LocalDateTime capturedCutoff = dateCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusHours(12);

        long diffInSeconds = ChronoUnit.SECONDS.between(capturedCutoff, expectedCutoff);

        assertTrue(Math.abs(diffInSeconds) < 1,
                "The cutoff time should be exactly 12 hours ago (within 1 second tolerance)");
    }
}