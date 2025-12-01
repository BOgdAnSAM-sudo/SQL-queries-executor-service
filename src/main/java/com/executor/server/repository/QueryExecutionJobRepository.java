package com.executor.server.repository;

import com.executor.entity.QueryExecutionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryExecutionJobRepository extends JpaRepository<QueryExecutionJob, Long> {
    List<QueryExecutionJob> findAllByOrderByIdDesc();
}
