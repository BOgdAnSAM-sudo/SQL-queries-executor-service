package com.executor.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QueryValidationServiceTest {

    @InjectMocks
    private QueryValidationService queryValidationService;

    // Valid SELECT queries that should pass validation
    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM titanic",
            "SELECT name, age FROM titanic",
            "SELECT COUNT(*) FROM titanic WHERE survived = 1",
            "SELECT name FROM titanic WHERE age > 18 ORDER BY name",
            "SELECT pclass, AVG(age) FROM titanic GROUP BY pclass",
            "SELECT * FROM titanic LIMIT 10",
            "select name from titanic",
            "Select * From titanic",
            "  SELECT * FROM titanic  ",
            "SELECT\n*\nFROM\ntitanic",
            "SELECT t.name FROM titanic t WHERE t.age > 20",
            "SELECT * FROM titanic WHERE name LIKE 'John%'",
            "SELECT * FROM titanic WHERE fare BETWEEN 10 AND 50",
            "SELECT * FROM titanic WHERE pclass IN (1, 2)",
            "SELECT DISTINCT pclass FROM titanic"
    })
    void validateQuery_ValidSelectQueries_DoesNotThrowException(String validQuery) {
        assertDoesNotThrow(() -> queryValidationService.validateQuery(validQuery));
    }

    // Null and empty queries
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void validateQuery_NullOrEmptyQuery_ThrowsSecurityException(String invalidQuery) {
        SecurityException exception = assertThrows(SecurityException.class,
                () -> queryValidationService.validateQuery(invalidQuery));

        assertEquals("Query cannot be empty", exception.getMessage());
    }


    // Non-SELECT queries - These should throw SecurityException with "Only select queries allowed"
    @ParameterizedTest
    @ValueSource(strings = {
            "DROP TABLE titanic",
            "DELETE FROM titanic",
            "UPDATE titanic SET name = 'John'",
            "INSERT INTO titanic VALUES (1, 'John')",
            "ALTER TABLE titanic ADD COLUMN test VARCHAR(255)",
            "CREATE TABLE test (id INT)",
            "TRUNCATE TABLE titanic",
            "EXECUTE sp_rename 'old_table', 'new_table'",
            "MERGE INTO target USING source ON condition",
            "CALL procedure_name()"
    })
    void validateQuery_NonSelectQueries_ThrowsSecurityException(String nonSelectQuery) {
        // Act & Assert
        System.out.println(nonSelectQuery);
        SecurityException exception = assertThrows(SecurityException.class,
                () -> queryValidationService.validateQuery(nonSelectQuery));

        assertEquals("Only retrieving data queries are allowed", exception.getMessage());
    }

    // Dangerous keywords in various contexts
    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM titanic; DROP TABLE titanic",
            "SELECT * FROM titanic WHERE name = 'test'; DELETE FROM titanic",
            "SELECT * FROM titanic; UPDATE titanic SET name = 'hacked'",
            "SELECT * FROM titanic; INSERT INTO titanic VALUES (999, 'hacker')",
            "SELECT * FROM titanic; ALTER TABLE titanic DROP COLUMN name",
            "SELECT * FROM titanic; CREATE TABLE malicious (data TEXT)",
            "SELECT * FROM titanic; TRUNCATE TABLE titanic",
    })
    void validateQuery_QueriesWithMultipleStatements_ThrowsSecurityException(String dangerousQuery) {
        SecurityException exception = assertThrows(SecurityException.class,
                () -> queryValidationService.validateQuery(dangerousQuery));

        assertEquals("Query should contain only one statement", exception.getMessage());
    }


    @Test
    void validateQuery_DeleteWithSemicolon_ThrowsSecurityException() {
        String query = "SELECT * FROM titanic;DELETE";

        SecurityException exception = assertThrows(SecurityException.class,
                () -> queryValidationService.validateQuery(query));

        assertEquals("Query should contain only one statement", exception.getMessage());
    }

    @Test
    void validateQuery_MultipleDangerousOperations_ThrowsSecurityException() {
        String query = "SELECT * FROM titanic; DROP TABLE users; DELETE FROM logs";

        SecurityException exception = assertThrows(SecurityException.class,
                () -> queryValidationService.validateQuery(query));

        assertEquals("Query should contain only one statement", exception.getMessage());
    }

    // Case sensitivity tests - These should throw SecurityException now
    @ParameterizedTest
    @ValueSource(strings = {
            "drop TABLE titanic",
            "DELETE from titanic",
            "update titanic SET name = 'test'",
            "insert INTO titanic VALUES (1)",
            "alter TABLE titanic",
            "create TABLE test",
            "truncate TABLE titanic"
    })
    void validateQuery_DangerousKeywordsCaseInsensitive_ThrowsSecurityException(String caseInsensitiveQuery) {
        SecurityException exception = assertThrows(SecurityException.class,
                () -> queryValidationService.validateQuery(caseInsensitiveQuery));

        assertEquals("Only retrieving data queries are allowed", exception.getMessage());
    }

    // Complex valid queries that should pass
    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT t1.name, t2.age FROM titanic t1 JOIN other_table t2 ON t1.id = t2.id",
            "SELECT name, (SELECT COUNT(*) FROM other_table) as count FROM titanic",
            "SELECT name FROM titanic WHERE age > (SELECT AVG(age) FROM titanic)",
            "SELECT pclass, COUNT(*) as count FROM titanic GROUP BY pclass HAVING COUNT(*) > 10",
            "SELECT * FROM titanic ORDER BY name DESC, age ASC",
            "SELECT COALESCE(name, 'Unknown') as name FROM titanic",
            "SELECT UPPER(name) as uppercase_name FROM titanic",
            "SELECT * FROM titanic WHERE created_date > '2023-01-01'",
            "SELECT name, ROUND(fare, 2) as rounded_fare FROM titanic"
    })
    void validateQuery_ComplexValidQueries_DoesNotThrowException(String complexQuery) {
        assertDoesNotThrow(() -> queryValidationService.validateQuery(complexQuery));
    }


    // Whitespace handling
    @Test
    void validateQuery_QueryWithLeadingTrailingWhitespace_ValidatesCorrectly() {
        String query = "   SELECT * FROM titanic   ";

        assertDoesNotThrow(() -> queryValidationService.validateQuery(query));
    }

    @Test
    void validateQuery_QueryWithTabsAndNewlines_ValidatesCorrectly() {
        String query = "SELECT\t*\nFROM\ttitanic\nWHERE\tage > 18";

        assertDoesNotThrow(() -> queryValidationService.validateQuery(query));
    }

    @Test
    void validateQuery_KeywordAsSubstringOfLargerWord_DoesNotThrowException() {
        String query = "SELECT dropdown, deleted_flag FROM settings";

        assertDoesNotThrow(() -> queryValidationService.validateQuery(query));
    }

    @Test
    void validateQuery_TableNameStartingWithDangerousKeyword_DoesNotThrowException() {
        String query = "SELECT * FROM dropdown_menu";

        assertDoesNotThrow(() -> queryValidationService.validateQuery(query));
    }

    @Test
    void validateQuery_VeryLongValidQuery_DoesNotThrowException() {
        StringBuilder longQuery = new StringBuilder("SELECT ");
        for (int i = 0; i < 100; i++) {
            longQuery.append("column").append(i).append(", ");
        }
        longQuery.append("id FROM very_large_table WHERE condition = 'value'");

        assertDoesNotThrow(() -> queryValidationService.validateQuery(longQuery.toString()));
    }

    @Test
    void validateQuery_QueryWithUnicodeCharacters_ValidatesCorrectly() {
        String query = "SELECT name FROM titanic WHERE name = 'José'";

        assertDoesNotThrow(() -> queryValidationService.validateQuery(query));
    }


    @Test
    void validateQuery_QueryWithUnbalancedQuotes_ThrowsException() {
        String query = "SELECT * FROM titanic WHERE name = 'test";

        assertThrows(RuntimeException.class,
                () -> queryValidationService.validateQuery(query));
    }
}