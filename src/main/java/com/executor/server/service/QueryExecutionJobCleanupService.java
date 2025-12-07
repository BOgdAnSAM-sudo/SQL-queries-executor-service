package com.executor.server.service;

import com.executor.server.repository.QueryExecutionJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

@Service
public class QueryExecutionJobCleanupService {
    private static final Logger log = LoggerFactory.getLogger(QueryExecutionJobCleanupService.class);
    private final QueryExecutionJobRepository repository;

    private static final int RETENTION_HOURS = 12;

    public QueryExecutionJobCleanupService(QueryExecutionJobRepository repository) {
        this.repository = repository;
    }

    // fixedRate = 3600000 ms (1 hour)
    @Scheduled(fixedRate = 3600000)
    public void cleanupOldJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(RETENTION_HOURS);

        log.info("Starting cleanup of jobs older than {}", cutoff);

        int deletedCount = repository.deleteOlderThan(cutoff);

        log.info("Cleanup finished. Deleted {} old job entities.", deletedCount);
    }
}
