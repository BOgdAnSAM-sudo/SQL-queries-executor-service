package com.test_task.jetbrains_internship_test_task.server.service;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.repository.StoredQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StoredQueryServiceTest {
    @Mock
    private StoredQueryRepository queryRepository;

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
    public void addQueryTest() {
        when(queryRepository.save(any(StoredQuery.class))).thenReturn(storedQuery);

        StoredQuery result = queryService.addQuery(queryText);

        assertEquals(queryText, result.getQuery());
        verify(queryRepository).save(any(StoredQuery.class));
    }

    @Test
    public void addNullQueryTest() {
        try {
            queryService.addQuery(null);
        }
        catch (StoredQueryException e){
            assertEquals("Query cannot be null", e.getMessage());
        }

    }


    @Test
    public void getQueryByIdTest() {
        when(queryRepository.findById(queryId)).thenReturn(Optional.of(storedQuery));

        Optional<StoredQuery> result = queryService.getQueryById(queryId);

        assertEquals(Optional.of(storedQuery), result);

        verify(queryRepository).findById(queryId);
    }

}
