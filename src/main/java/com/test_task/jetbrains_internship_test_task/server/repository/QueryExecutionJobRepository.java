package com.test_task.jetbrains_internship_test_task.server.repository;

import com.test_task.jetbrains_internship_test_task.entity.QueryExecutionJob;
import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QueryExecutionJobRepository extends JpaRepository<QueryExecutionJob, Long> {
    List<QueryExecutionJob> findAllByOrderByIdDesc();
}
