package com.executor.server.service;

import com.executor.entity.StoredQuery;
import com.executor.server.repository.StoredQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoredQueryServiceTest {
    @Mock
    private StoredQueryRepository queryRepository;

    @Mock
    private QueryValidationService validationService;

    @InjectMocks
    private StoredQueryService queryService;

    private Long queryId;
    private String queryText;
    private StoredQuery storedQuery;

    @BeforeEach
    public void setUp(){
        queryId = 1L;
        queryText = "SELECT name FROM passengers";
        storedQuery = new StoredQuery(queryText);
        storedQuery.setId(queryId);
    }

    @Test
    public void getAllQueriesTest(){
        List<StoredQuery> queries = new ArrayList<>();
        queries.add(storedQuery);
        when(queryRepository.findAllByOrderByCreatedAtDesc()).thenReturn(queries);

        List<StoredQuery> result = queryService.getAllQueries();

        assertEquals(queries,result);

        verify(queryRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    public void getAllQueries_EmptyList_ReturnsEmptyList() {
        when(queryRepository.findAllByOrderByCreatedAtDesc()).thenReturn(new ArrayList<>());

        List<StoredQuery> result = queryService.getAllQueries();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(queryRepository).findAllByOrderByCreatedAtDesc();
    }


    @Test
    public void addQueryTest() {
        when(queryRepository.save(any(StoredQuery.class))).thenReturn(storedQuery);

        StoredQuery result = queryService.addQuery(queryText);

        assertEquals(queryText, result.getQuery());
        verify(queryRepository).save(any(StoredQuery.class));
    }

    @Test
    public void addQuery_ComplexQuery_SavesSuccessfully() {
        String complexQuery = """
            SELECT p.name, p.age, COUNT(*) as count 
            FROM passengers p 
            WHERE p.age > 18 
            GROUP BY p.name, p.age 
            HAVING COUNT(*) > 1
            ORDER BY p.name
            """;
        StoredQuery complexStoredQuery = new StoredQuery(complexQuery);
        complexStoredQuery.setId(3L);

        when(queryRepository.save(any(StoredQuery.class))).thenReturn(complexStoredQuery);

        StoredQuery result = queryService.addQuery(complexQuery);

        assertNotNull(result);
        assertEquals(complexQuery, result.getQuery());
        assertEquals(3L, result.getId());
        verify(queryRepository).save(any(StoredQuery.class));
    }

    @Test
    public void addNullQueryTest() {
        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryService.addQuery(null));

        assertEquals("Query cannot be null", exception.getMessage());
        verify(queryRepository, never()).save(any(StoredQuery.class));
    }


    @Test
    public void getQueryByIdTest() {
        when(queryRepository.findById(queryId)).thenReturn(Optional.of(storedQuery));

        Optional<StoredQuery> result = queryService.getQueryById(queryId);

        assertEquals(Optional.of(storedQuery), result);

        verify(queryRepository).findById(queryId);
    }


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    public void addQuery_InvalidQueryText_ThrowsException(String invalidQuery) {
        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryService.addQuery(invalidQuery));

        assertEquals("Query cannot be null", exception.getMessage());
        verify(queryRepository, never()).save(any(StoredQuery.class));
    }

    @Test
    public void getQueryById_NonExistentId_ReturnsEmptyOptional() {
        Long nonExistentId = 999L;
        when(queryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        
        Optional<StoredQuery> result = queryService.getQueryById(nonExistentId);

        assertTrue(result.isEmpty());
        verify(queryRepository).findById(nonExistentId);
    }

    @Test
    public void getQueryById_NullId_ReturnsEmptyOptional() {
        when(queryRepository.findById(null)).thenReturn(Optional.empty());

        Optional<StoredQuery> result = queryService.getQueryById(null);

        assertTrue(result.isEmpty());
        verify(queryRepository).findById(null);
    }

    @Test
    public void addQuery_RepositoryThrowsException_PropagatesException() {
        when(queryRepository.save(any(StoredQuery.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class,
                () -> queryService.addQuery(queryText));

        verify(queryRepository).save(any(StoredQuery.class));
    }

    @Test
    public void getAllQueries_RepositoryThrowsException_PropagatesException() {
        when(queryRepository.findAllByOrderByCreatedAtDesc())
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class,
                () -> queryService.getAllQueries());

        verify(queryRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    public void addQuery_VerifyObjectCreation() {
        when(queryRepository.save(any(StoredQuery.class))).thenAnswer(invocation -> {
            StoredQuery savedQuery = invocation.getArgument(0);
            assertNotNull(savedQuery.getCreatedAt());
            assertEquals(queryText, savedQuery.getQuery());
            savedQuery.setId(queryId);
            return savedQuery;
        });

        StoredQuery result = queryService.addQuery(queryText);
        
        assertNotNull(result);
        assertEquals(queryId, result.getId());
        assertEquals(queryText, result.getQuery());
        assertNotNull(result.getCreatedAt());
    }

    @ParameterizedTest
    @MethodSource("provideValidQueries")
    public void addQuery_VariousValidQueries_SavesSuccessfully(String validQuery) {
        StoredQuery mockStoredQuery = new StoredQuery(validQuery);
        mockStoredQuery.setId(100L);

        when(queryRepository.save(any(StoredQuery.class))).thenReturn(mockStoredQuery);

        StoredQuery result = queryService.addQuery(validQuery);

        assertNotNull(result);
        assertEquals(validQuery, result.getQuery());
        verify(queryRepository).save(any(StoredQuery.class));
    }

    private static Stream<Arguments> provideValidQueries() {
        return Stream.of(
                Arguments.of("SELECT * FROM table"),
                Arguments.of("SELECT col1, col2 FROM table WHERE condition = 'value'"),
                Arguments.of("SELECT COUNT(*) as count FROM large_table"),
                Arguments.of("SELECT\n    multi_line\nFROM\n    query"),
                Arguments.of("SELECT * FROM table -- with comment")
        );
    }
}
