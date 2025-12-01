package com.executor.server.service;

import com.executor.entity.QueryExecutionJob;
import com.executor.server.repository.QueryExecutionJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class QueryExecutionJobService {
    private final QueryExecutionJobRepository jobRepository;

    public QueryExecutionJobService(QueryExecutionJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public QueryExecutionJob addJob(Long queryId){
        if (queryId == null) {
            throw new QueryExecutionJobException("Query cannot be null");
        }

        QueryExecutionJob queryExecutionJob = new QueryExecutionJob();
        queryExecutionJob.setSourceQueryId(queryId);
        queryExecutionJob.setStatus(QueryExecutionJob.JobStatus.PENDING);
        return jobRepository.save(queryExecutionJob);
    }

    public List<QueryExecutionJob> getAllJobs() {
        return jobRepository.findAllByOrderByIdDesc();
    }

    public Optional<QueryExecutionJob> getJobById(Long id) {
        return jobRepository.findById(id);
    }
}
