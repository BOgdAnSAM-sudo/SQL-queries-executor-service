package com.executor.server.repository;

import com.executor.entity.QueryExecutionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryExecutionJobRepository extends JpaRepository<QueryExecutionJob, Long> {
    List<QueryExecutionJob> findAllByOrderByIdDesc();

    @Modifying
    @Transactional
    @Query("DELETE FROM QueryExecutionJob j WHERE j.createdAt < :cutoffTime")
    int deleteOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
}
