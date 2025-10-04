package com.test_task.jetbrains_internship_test_task.server.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class QueryExecutionRepository {
    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeNativeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        return jdbcTemplate.queryForList(query);
    }
}
