package com.executor.server.service;

import com.executor.entity.StoredQuery;
import com.executor.server.repository.StoredQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StoredQueryServiceIntegrationTest {

    @Autowired
    private StoredQueryService queryService;

    @Autowired
    private StoredQueryRepository queryRepository;

    private final String simpleQuery = "SELECT name FROM passengers WHERE age > 18";
    private final String complexQuery = """
            SELECT p.name, p.age, COUNT(*) as count 
            FROM passengers p 
            WHERE p.age > 18 
            GROUP BY p.name, p.age 
            HAVING COUNT(*) > 1
            ORDER BY p.name
            """;;

    @Test
    public void addQuery_ValidQuery_SavesToDatabase() {
        StoredQuery savedQuery = queryService.addQuery(simpleQuery);

        assertNotNull(savedQuery);
        assertNotNull(savedQuery.getId());
        assertEquals(simpleQuery, savedQuery.getQuery());
        assertNotNull(savedQuery.getCreatedAt());

        Optional<StoredQuery> retrievedQuery = queryRepository.findById(savedQuery.getId());
        assertTrue(retrievedQuery.isPresent());
        assertEquals(savedQuery.getId(), retrievedQuery.get().getId());
        assertEquals(simpleQuery, retrievedQuery.get().getQuery());
    }

    @Test
    public void addQuery_ComplexQuery_SavesToDatabase() {
        StoredQuery savedQuery = queryService.addQuery(complexQuery);

        assertNotNull(savedQuery);
        assertNotNull(savedQuery.getId());
        assertEquals(complexQuery, savedQuery.getQuery());

        Optional<StoredQuery> retrievedQuery = queryService.getQueryById(savedQuery.getId());
        assertTrue(retrievedQuery.isPresent());
        assertEquals(complexQuery, retrievedQuery.get().getQuery());
    }

    @Test
    public void addQuery_MultipleQueries_SavesAllToDatabase() {
        StoredQuery query1 = queryService.addQuery(simpleQuery);
        StoredQuery query2 = queryService.addQuery(complexQuery);
        StoredQuery query3 = queryService.addQuery("SELECT COUNT(*) FROM passengers");

        assertNotNull(query1.getId());
        assertNotNull(query2.getId());
        assertNotNull(query3.getId());

        List<StoredQuery> allQueries = queryService.getAllQueries();
        assertEquals(3, allQueries.size());

        assertNotEquals(query1.getId(), query2.getId());
        assertNotEquals(query1.getId(), query3.getId());
        assertNotEquals(query2.getId(), query3.getId());
    }

    @Test
    public void getAllQueries_EmptyDatabase_ReturnsEmptyList() {
        List<StoredQuery> queries = queryService.getAllQueries();

        assertNotNull(queries);
        assertTrue(queries.isEmpty());
    }

    @Test
    public void getAllQueries_MultipleQueries_ReturnsAllInCorrectOrder() {
        StoredQuery query1 = queryService.addQuery(simpleQuery);
        // Small delay to ensure different timestamps
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        StoredQuery query2 = queryService.addQuery(complexQuery);


        List<StoredQuery> queries = queryService.getAllQueries();

        assertEquals(2, queries.size());

        assertEquals(query2.getId(), queries.get(0).getId());
        assertEquals(query1.getId(), queries.get(1).getId());

        assertTrue(queries.get(0).getCreatedAt().isAfter(queries.get(1).getCreatedAt()));
    }

    @Test
    public void getQueryById_ExistingId_ReturnsCorrectQuery() {
        StoredQuery savedQuery = queryService.addQuery(simpleQuery);
        Long savedId = savedQuery.getId();

        Optional<StoredQuery> retrievedQuery = queryService.getQueryById(savedId);

        assertTrue(retrievedQuery.isPresent());
        assertEquals(savedId, retrievedQuery.get().getId());
        assertEquals(simpleQuery, retrievedQuery.get().getQuery());
    }

    @Test
    public void getQueryById_NonExistentId_ReturnsEmptyOptional() {
        Optional<StoredQuery> retrievedQuery = queryService.getQueryById(999L);

        assertTrue(retrievedQuery.isEmpty());
    }

    @Test
    public void addQuery_NullQuery_ThrowsStoredQueryException() {
        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryService.addQuery(null));

        assertEquals("Query cannot be null", exception.getMessage());

        assertEquals(0, queryRepository.count());
    }

    @Test
    public void addQuery_EmptyQuery_ThrowsStoredQueryException() {
        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryService.addQuery(""));

        assertEquals("Query cannot be null", exception.getMessage());

        assertEquals(0, queryRepository.count());
    }

    @Test
    public void addQuery_BlankQuery_ThrowsStoredQueryException() {
        StoredQueryException exception = assertThrows(StoredQueryException.class,
                () -> queryService.addQuery("   "));

        assertEquals("Query cannot be null", exception.getMessage());

        assertEquals(0, queryRepository.count());
    }

    @Test
    public void queryLifecycle_CompleteFlow_Success() {
        StoredQuery query1 = queryService.addQuery("SELECT * FROM table1");
        StoredQuery query2 = queryService.addQuery("SELECT * FROM table2");
        StoredQuery query3 = queryService.addQuery("SELECT * FROM table3");

        List<StoredQuery> allQueries = queryService.getAllQueries();
        assertEquals(3, allQueries.size());

        Optional<StoredQuery> retrievedQuery2 = queryService.getQueryById(query2.getId());
        assertTrue(retrievedQuery2.isPresent());
        assertEquals("SELECT * FROM table2", retrievedQuery2.get().getQuery());

        assertEquals(query3.getId(), allQueries.get(0).getId());
        assertEquals(query2.getId(), allQueries.get(1).getId());
        assertEquals(query1.getId(), allQueries.get(2).getId());
    }

}