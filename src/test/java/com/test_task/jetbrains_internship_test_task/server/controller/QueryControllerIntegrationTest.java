package com.test_task.jetbrains_internship_test_task.server.controller;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.service.StoredQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoredQueryService queryService;

    @Test
    public void storeQuery_ValidQuery_ReturnsId() throws Exception {
        String queryText = "SELECT * FROM passengers WHERE age > 18";
        
        mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(queryText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
        
        List<StoredQuery> storedQueries = queryService.getAllQueries();
        assertEquals(1, storedQueries.size());
        assertEquals(queryText, storedQueries.getFirst().getQuery());
    }

    @Test
    public void storeQuery_EmptyQuery_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isBadRequest());

        List<StoredQuery> storedQueries = queryService.getAllQueries();
        assertTrue(storedQueries.isEmpty());
    }

    @Test
    public void storeQuery_WithSpecialCharacters_HandlesCorrectly() throws Exception {
        String complexQuery = "SELECT * FROM table WHERE name = John`s Café";

        mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(complexQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        List<StoredQuery> storedQueries = queryService.getAllQueries();
        assertEquals(1, storedQueries.size());
        assertEquals(complexQuery, storedQueries.getFirst().getQuery());
    }

    @Test
    public void storeQuery_MultipleQueries_AllStoredSuccessfully() throws Exception {
        String query1 = "SELECT * FROM table1";
        String query2 = "SELECT name, age FROM table2";

        mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(query1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(query2))
                .andExpect(status().isOk());

        List<StoredQuery> storedQueries = queryService.getAllQueries();
        assertEquals(2, storedQueries.size());
        assertTrue(storedQueries.stream().anyMatch(q -> query1.equals(q.getQuery())));
        assertTrue(storedQueries.stream().anyMatch(q -> query2.equals(q.getQuery())));
    }

    @Test
    public void getAllQueries_EmptyDatabase_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getAllQueries_WithStoredQueries_ReturnsAllQueries() throws Exception {
        StoredQuery query1 = queryService.addQuery("SELECT * FROM table1");
        StoredQuery query2 = queryService.addQuery("SELECT name FROM table2");

        mockMvc.perform(get("/api/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(query2.getId()))
                .andExpect(jsonPath("$[0].query").value("SELECT name FROM table2"))
                .andExpect(jsonPath("$[1].id").value(query1.getId()))
                .andExpect(jsonPath("$[1].query").value("SELECT * FROM table1"));
    }

    @Test
    public void executeQuery_ValidId_ReturnsResults() throws Exception {
        StoredQuery storedQuery = queryService.addQuery("SELECT Name, Age FROM titanic WHERE CAST(PassengerId AS INTEGER) <= 3");
        Long queryId = storedQuery.getId();

        mockMvc.perform(get("/api/execute")
                        .param("queryId", queryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3)) // Should return 3 passengers
                .andExpect(jsonPath("$[0].length()").value(2)); // Each row has 2 columns
    }

    @Test
    public void executeQuery_NonExistentId_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/execute")
                        .param("queryId", "999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void executeQuery_MissingQueryId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/execute"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void executeQuery_InvalidQueryId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/execute")
                        .param("queryId", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void completeWorkflow_StoreAndExecute_Success() throws Exception {
        String analyticalQuery = "SELECT Pclass, COUNT(*) as total FROM titanic GROUP BY Pclass ORDER BY Pclass";

        String response = mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(analyticalQuery))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long queryId = Long.parseLong(response.split("\"id\":")[1].replaceAll("[^0-9]", ""));

        mockMvc.perform(get("/api/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(queryId))
                .andExpect(jsonPath("$[0].query").value(analyticalQuery));

        mockMvc.perform(get("/api/execute")
                        .param("queryId", queryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3)) // 3 passenger classes
                .andExpect(jsonPath("$[0].length()").value(2)); // Pclass and count
    }

    @Test
    public void contentType_StoreQuery_WrongContentType_ReturnsUnsupported() throws Exception {
        mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"SELECT 1\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }


    @Test
    public void storeQuery_WithNewlines_HandlesCorrectly() throws Exception {
        String multilineQuery = "SELECT \n" +
                "    name,\n" +
                "    age\n" +
                "FROM \n" +
                "    passengers\n" +
                "WHERE \n" +
                "    age > 18";

        mockMvc.perform(post("/api/queries")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(multilineQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        List<StoredQuery> storedQueries = queryService.getAllQueries();
        assertEquals(1, storedQueries.size());
        assertEquals(multilineQuery, storedQueries.getFirst().getQuery());
    }
}