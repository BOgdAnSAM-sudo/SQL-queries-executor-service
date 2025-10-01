package com.test_task.jetbrains_internship_test_task.server.service;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.repository.StoredQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueryExecutionServiceIntegrationTest {

    @Autowired
    private QueryExecutionService queryExecutionService;

    @Autowired
    private StoredQueryService storedQueryService;

    @Autowired
    private StoredQueryRepository storedQueryRepository;

    @BeforeEach
    public void setUp() {
        storedQueryRepository.deleteAll();
    }

    @Test
    public void executeQuery_SimpleSelectFromTitanic_ReturnsResults() {
        String query = "SELECT Name, Age, Survived FROM titanic WHERE CAST(PassengerId AS INTEGER) <= 5 ORDER BY CAST(PassengerId AS INTEGER)";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertEquals(5, result.size());

        List<Object> firstRow = result.getFirst();
        assertEquals(3, firstRow.size());
        assertEquals("Braund, Mr. Owen Harris", firstRow.get(0));
        assertEquals("22", firstRow.get(1));
        assertEquals("0", firstRow.get(2));
    }

    @Test
    public void executeQuery_CountSurvivors_ReturnsCorrectCount() {
        String query = "SELECT COUNT(*) as survivor_count FROM titanic WHERE CAST(Survived AS INTEGER) = 1";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().size());

        Long survivorCount = (Long) result.getFirst().getFirst();
        assertTrue(survivorCount > 0);
    }

    @Test
    public void executeQuery_SurvivalRateByClass_ReturnsAggregatedResults() {
        String query = "SELECT CAST(Pclass AS INTEGER) as pclass, " +
                "COUNT(*) as total, " +
                "SUM(CAST(Survived AS INTEGER)) as survivors, " +
                "ROUND(SUM(CAST(Survived AS INTEGER)) * 100.0 / COUNT(*), 2) as survival_rate " +
                "FROM titanic " +
                "GROUP BY Pclass " +
                "ORDER BY CAST(Pclass AS INTEGER)";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should return results for at least one passenger class");

        result.forEach(row -> assertEquals(4, row.size()));

        assertEquals(1, ((Number) result.get(0).getFirst()).intValue());
        assertEquals(2, ((Number) result.get(1).getFirst()).intValue());
        assertEquals(3, ((Number) result.get(2).getFirst()).intValue());
    }

    @Test
    public void executeQuery_AverageFareByClassAndSurvival_ReturnsCorrectAverages() {
        String query = "SELECT CAST(Pclass AS INTEGER) as pclass, CAST(Survived AS INTEGER) as survived, AVG(CAST(Fare AS DOUBLE)) as avg_fare " +
                "FROM titanic " +
                "WHERE CAST(Fare AS DOUBLE) > 0 " +
                "GROUP BY Pclass, Survived " +
                "ORDER BY CAST(Pclass AS INTEGER), CAST(Survived AS INTEGER)";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertTrue(result.size() >= 2);

        result.forEach(row -> {
            assertEquals(3, row.size());
            assertInstanceOf(Number.class, row.get(0), "Pclass should be numeric");
            assertInstanceOf(Number.class, row.get(1), "Survived should be numeric");
            assertInstanceOf(Number.class, row.get(2), "avg_fare should be numeric");
        });
    }


    @Test
    public void executeQuery_TopExpensiveTickets_ReturnsHighestFares() {
        String query = "SELECT Name, CAST(Pclass AS INTEGER) as pclass, CAST(Fare AS DOUBLE) as fare " +
                "FROM titanic " +
                "WHERE CAST(Fare AS DOUBLE) > 0 " +
                "ORDER BY CAST(Fare AS DOUBLE) DESC " +
                "LIMIT 5";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertEquals(5, result.size());

        for (int i = 0; i < result.size() - 1; i++) {
            Double currentFare = (Double) result.get(i).get(2);
            Double nextFare = (Double) result.get(i + 1).get(2);
            assertTrue(currentFare >= nextFare);
        }
    }

    @Test
    public void executeQuery_PassengersWithMissingAge_ReturnsNullAges() {
        String query = "SELECT Name, Age " +
                "FROM titanic " +
                "WHERE Age IS NULL OR Age = '' " +
                "LIMIT 10";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);

        result.forEach(row -> {
            assertEquals(2, row.size());
            assertNotNull(row.get(0));
            assertTrue(row.get(1) == null || "".equals(row.get(1)));
        });
    }

    @Test
    public void executeQueryById_StoredTitanicQuery_ExecutesSuccessfully() {
        String complexQuery = "SELECT CAST(Pclass AS INTEGER) as pclass, Sex, " +
                "COUNT(*) as total, " +
                "SUM(CAST(Survived AS INTEGER)) as survivors, " +
                "ROUND(AVG(CAST(Fare AS DOUBLE)), 2) as avg_fare " +
                "FROM titanic " +
                "WHERE Age IS NOT NULL " +
                "GROUP BY Pclass, Sex " +
                "ORDER BY CAST(Pclass AS INTEGER), Sex";

        StoredQuery storedQuery = storedQueryService.addQuery(complexQuery);
        Long queryId = storedQuery.getId();

        List<List<Object>> result = queryExecutionService.executeQueryById(queryId);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        List<Object> firstRow = result.getFirst();
        assertEquals(5, firstRow.size()); // Pclass, Sex, total, survivors, avg_fare
    }

    @Test
    public void executeQuery_ComplexJoinOnFamilySize_ReturnsFamilyAnalysis() {
        String query = "SELECT " +
                "family_size, " +
                "COUNT(*) as passenger_count, " +
                "SUM(survived) as survivors, " +
                "ROUND(SUM(survived) * 100.0 / COUNT(*), 2) as survival_rate " +
                "FROM ( " +
                "    SELECT " +
                "        PassengerId, " +
                "        CAST(Survived AS INTEGER) as survived, " +
                "        (CAST(SibSp AS INTEGER) + CAST(Parch AS INTEGER) + 1) as family_size " +
                "    FROM titanic " +
                ") as family_data " +
                "GROUP BY family_size " +
                "HAVING COUNT(*) > 5 " + // Only consider family sizes with enough data
                "ORDER BY family_size";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);

        assertFalse(result.isEmpty());
    }


    @Test
    public void executeQuery_EmptyResult_WhenNoMatches() {
        String query = "SELECT * FROM titanic WHERE CAST(Age AS DOUBLE) > 100";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void executeQuery_WithMultipleConditions_ReturnsFilteredResults() {
        String query = "SELECT Name, Age, CAST(Pclass AS INTEGER) as pclass, CAST(Fare AS DOUBLE) as fare " +
                "FROM titanic " +
                "WHERE CAST(Survived AS INTEGER) = 1 " +
                "AND CAST(Pclass AS INTEGER) = 1 " +
                "AND CAST(Age AS DOUBLE) BETWEEN 20 AND 40 " +
                "AND CAST(Fare AS DOUBLE) > 50 " +
                "ORDER BY CAST(Fare AS DOUBLE) DESC";

        List<List<Object>> result = queryExecutionService.executeQuery(query);

        assertNotNull(result);
        // May or may not have results depending on data
        // Just verify no exception and proper structure
        result.forEach(row -> assertEquals(4, row.size()));
    }

    @Test
    public void executeQuery_InvalidColumn_ThrowsException() {
        String invalidQuery = "SELECT NonExistentColumn FROM titanic";

        assertThrows(BadSqlGrammarException.class,
                () -> queryExecutionService.executeQuery(invalidQuery));
    }

    @Test
    public void executeQuery_SyntaxError_ThrowsException() {
        String syntaxError = "SELECT FROM WHERE titanic";

        assertThrows(BadSqlGrammarException.class,
                () -> queryExecutionService.executeQuery(syntaxError));
    }

    @Test
    public void executeQueryById_NonExistentId_ReturnsNull() {
        List<List<Object>> result = queryExecutionService.executeQueryById(999L);

        assertNull(result);
    }
}