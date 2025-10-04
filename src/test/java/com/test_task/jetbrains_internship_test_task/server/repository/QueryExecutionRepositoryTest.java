package com.test_task.jetbrains_internship_test_task.server.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryExecutionRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private QueryExecutionRepository queryExecutionRepository;

    @Test
    void executeNativeQuery_ValidQuery_ReturnsResults() {
        // Arrange
        String query = "SELECT * FROM titanic";
        List<Map<String, Object>> expectedResults = List.of(
                Map.of("id", 1, "name", "John"),
                Map.of("id", 2, "name", "Jane")
        );

        when(jdbcTemplate.queryForList(query)).thenReturn(expectedResults);

        List<Map<String, Object>> results = queryExecutionRepository.executeNativeQuery(query);

        assertEquals(expectedResults, results);
        verify(jdbcTemplate).queryForList(query);
    }

    @Test
    void executeNativeQuery_JdbcTemplateThrowsException_PropagatesException() {
        String query = "INVALID SQL";
        when(jdbcTemplate.queryForList(query))
                .thenThrow(new RuntimeException("SQL syntax error"));

        assertThrows(RuntimeException.class,
                () -> queryExecutionRepository.executeNativeQuery(query));
    }

    @Test
    void executeNativeQuery_NullQuery_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> queryExecutionRepository.executeNativeQuery(null));
    }

    @Test
    void executeNativeQuery_EmptyQuery_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> queryExecutionRepository.executeNativeQuery(""));
    }
}