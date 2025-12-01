package com.executor.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.executor.entity.QueryExecutionJob;
import com.executor.entity.StoredQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueryExecutionServiceIntegrationTest {

    @Autowired
    private QueryExecutionService queryExecutionService; // Let Spring inject it

    @Autowired
    private StoredQueryService storedQueryService;

    @Autowired
    private QueryExecutionJobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    private QueryExecutionJob waitForJobCompletion(Long jobId, long timeoutSeconds) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000) {
            QueryExecutionJob job = jobService.getJobById(jobId).orElseThrow();
            if (job.getStatus() == QueryExecutionJob.JobStatus.COMPLETED ||
                    job.getStatus() == QueryExecutionJob.JobStatus.FAILED) {
                return job;
            }
            Thread.sleep(50);
        }
        throw new RuntimeException("Job did not complete within " + timeoutSeconds + " seconds");
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> parseJobResult(QueryExecutionJob job) {
        try {
            return objectMapper.readValue(job.getResult(), List.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse job result", e);
        }
    }

    @Test
    public void executeQuery_WithSourceQueryId_ExecutesSuccessfully() throws Exception {
        String queryText = "SELECT COUNT(*) FROM titanic";
        StoredQuery storedQuery = storedQueryService.addQuery(queryText);

        QueryExecutionJob job = jobService.addJob(storedQuery.getId());
        Long jobId = job.getId();

        queryExecutionService.executeQuery(jobId);
        QueryExecutionJob completedJob = waitForJobCompletion(jobId, 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob.getStatus());
        assertEquals(storedQuery.getId(), completedJob.getSourceQueryId());

        List<List<Object>> result = parseJobResult(completedJob);
        assertNotNull(result);
        assertEquals(1, result.size());

        Number count = (Number) result.getFirst().getFirst();
        assertTrue(count.longValue() > 0);
    }

    @Test
    public void executeQuery_CountSurvivors_ReturnsCorrectCount() throws Exception {
        String queryText = "SELECT COUNT(*) as survivor_count FROM titanic WHERE CAST(Survived AS INTEGER) = 1";
        StoredQuery storedQuery = storedQueryService.addQuery(queryText);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob completedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob.getStatus());
        List<List<Object>> result = parseJobResult(completedJob);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().size());

        Number survivorCount = (Number) result.getFirst().getFirst();
        assertTrue(survivorCount.longValue() > 0);
    }

    @Test
    public void executeQuery_SurvivalRateByClass_ReturnsAggregatedResults() throws Exception {
        String queryText = "SELECT CAST(Pclass AS INTEGER) as pclass, " +
                "COUNT(*) as total, " +
                "SUM(CAST(Survived AS INTEGER)) as survivors, " +
                "ROUND(SUM(CAST(Survived AS INTEGER)) * 100.0 / COUNT(*), 2) as survival_rate " +
                "FROM titanic " +
                "GROUP BY Pclass " +
                "ORDER BY CAST(Pclass AS INTEGER)";

        StoredQuery storedQuery = storedQueryService.addQuery(queryText);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob completedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob.getStatus());
        List<List<Object>> result = parseJobResult(completedJob);

        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should return results for at least one passenger class");

        result.forEach(row -> assertEquals(4, row.size()));

        assertEquals(1, ((Number) result.get(0).getFirst()).intValue());
        assertEquals(2, ((Number) result.get(1).getFirst()).intValue());
        assertEquals(3, ((Number) result.get(2).getFirst()).intValue());
    }

    @Test
    public void executeQuery_ComplexJoinOnFamilySize_ReturnsFamilyAnalysis() throws Exception {
        String queryText = "SELECT " +
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
                "HAVING COUNT(*) > 5 " +
                "ORDER BY family_size";

        StoredQuery storedQuery = storedQueryService.addQuery(queryText);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob completedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob.getStatus());
        List<List<Object>> result = parseJobResult(completedJob);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void executeQuery_EmptyResult_WhenNoMatches() throws Exception {
        String queryText = "SELECT * FROM titanic WHERE CAST(Age AS DOUBLE) > 100";

        StoredQuery storedQuery = storedQueryService.addQuery(queryText);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob completedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob.getStatus());
        List<List<Object>> result = parseJobResult(completedJob);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void executeQuery_WithMultipleConditions_ReturnsFilteredResults() throws Exception {
        // Arrange
        String queryText = "SELECT Name, Age, CAST(Pclass AS INTEGER) as pclass, CAST(Fare AS DOUBLE) as fare " +
                "FROM titanic " +
                "WHERE CAST(Survived AS INTEGER) = 1 " +
                "AND CAST(Pclass AS INTEGER) = 1 " +
                "AND CAST(Age AS DOUBLE) BETWEEN 20 AND 40 " +
                "AND CAST(Fare AS DOUBLE) > 50 " +
                "ORDER BY CAST(Fare AS DOUBLE) DESC";

        StoredQuery storedQuery = storedQueryService.addQuery(queryText);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob completedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob.getStatus());
        List<List<Object>> result = parseJobResult(completedJob);

        assertNotNull(result);
        result.forEach(row -> assertEquals(4, row.size()));
    }

    @Test
    public void executeQuery_InvalidColumn_FailsWithErrorMessage() throws Exception {
        String invalidQuery = "SELECT NonExistentColumn FROM titanic";
        StoredQuery storedQuery = storedQueryService.addQuery(invalidQuery);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob failedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.FAILED, failedJob.getStatus());
        assertNotNull(failedJob.getErrorMessage());
        assertTrue(failedJob.getErrorMessage().contains("NonExistentColumn") ||
                failedJob.getErrorMessage().contains("not found"));
    }

    @Test
    public void executeQuery_SyntaxError_FailsWithErrorMessage() throws Exception {
        String syntaxError = "SELECT FROM WHERE titanic";
        StoredQuery storedQuery = storedQueryService.addQuery(syntaxError);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob failedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.FAILED, failedJob.getStatus());
        assertNotNull(failedJob.getErrorMessage());
    }

    @Test
    public void executeQuery_ExecuteMultipleJobs() throws Exception {
        String query1 = "SELECT COUNT(*) FROM titanic WHERE CAST(Pclass AS INTEGER) = 1";
        String query2 = "SELECT COUNT(*) FROM titanic WHERE CAST(Pclass AS INTEGER) = 2";

        StoredQuery storedQuery1 = storedQueryService.addQuery(query1);
        StoredQuery storedQuery2 = storedQueryService.addQuery(query2);

        QueryExecutionJob job1 = jobService.addJob(storedQuery1.getId());
        QueryExecutionJob job2 = jobService.addJob(storedQuery2.getId());

        queryExecutionService.executeQuery(job1.getId());
        queryExecutionService.executeQuery(job2.getId());

        QueryExecutionJob completedJob1 = waitForJobCompletion(job1.getId(), 10);
        QueryExecutionJob completedJob2 = waitForJobCompletion(job2.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob1.getStatus());
        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob2.getStatus());

        List<List<Object>> result1 = parseJobResult(completedJob1);
        List<List<Object>> result2 = parseJobResult(completedJob2);

        assertNotNull(result1);
        assertNotNull(result2);

        Number count1 = (Number) result1.getFirst().getFirst();
        Number count2 = (Number) result2.getFirst().getFirst();

        assertTrue(count1.longValue() > 0);
        assertTrue(count2.longValue() > 0);
    }

    @Test
    public void executeQuery_JobLifecycle_TransitionsCorrectly() throws Exception {
        String queryText = "SELECT 1 as test_value";
        StoredQuery storedQuery = storedQueryService.addQuery(queryText);
        QueryExecutionJob job = jobService.addJob(storedQuery.getId());

        assertEquals(QueryExecutionJob.JobStatus.PENDING, job.getStatus());
        assertNull(job.getResult());
        assertNull(job.getErrorMessage());

        queryExecutionService.executeQuery(job.getId());
        QueryExecutionJob completedJob = waitForJobCompletion(job.getId(), 10);

        assertEquals(QueryExecutionJob.JobStatus.COMPLETED, completedJob.getStatus());
        assertNotNull(completedJob.getResult());
        assertNull(completedJob.getErrorMessage());

        List<List<Object>> result = parseJobResult(completedJob);
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().size());
        assertEquals(1, ((Number) result.getFirst().getFirst()).intValue());
    }
}