package com.executor.server.repository;

import com.executor.entity.StoredQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoredQueryRepository extends JpaRepository<StoredQuery, Long> {
    List<StoredQuery> findAllByOrderByCreatedAtDesc();
}
