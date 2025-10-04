package com.test_task.jetbrains_internship_test_task.server.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class QueryValidationService {

    public void validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new SecurityException("Query cannot be empty");
        }

        // Convert to uppercase for case-insensitive matching
        String upperQuery = query.toUpperCase().trim();

        String selectRegex = "\\b" + "SELECT" + "\\b";
        if (!Pattern.compile(selectRegex).matcher(upperQuery).find()) {
            throw new SecurityException("Only select queries allowed");
        }

        if (containsMultipleStatements(query)) {
            throw new SecurityException("Query contains potentially dangerous characters");
        }

        if (containsSqlInjectionPatterns(upperQuery)) {
            throw new SecurityException("Query contains potentially dangerous characters");
        }

        if (containsDangerousKeywords(upperQuery)) {
            throw new SecurityException("Query contains potentially dangerous characters");
        }

        if (!hasBalancedQuotes(query)) {
            throw new SecurityException("Query contains potentially dangerous characters");
        }
    }

    private boolean containsMultipleStatements(String query) {
        String temp = query.trim();
        if (temp.endsWith(";")) {
            temp = temp.substring(0, temp.length() - 1);
        }

        return temp.contains(";");
    }

    private boolean containsSqlInjectionPatterns(String upperQuery) {
        String[] injectionPatterns = {
                "OR '1'='1", "OR '1'='1'", "OR 1=1", "OR 'a'='a",
                "UNION SELECT", "UNION ALL SELECT",
                "--", "/*", "*/", "#",
                "DROP TABLE", "DELETE FROM", "INSERT INTO", "UPDATE ",
                "EXEC ", "EXECUTE ", "XP_", "SP_"
        };

        for (String pattern : injectionPatterns) {
            if (upperQuery.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDangerousKeywords(String upperQuery) {
        String regex = "\\b(UNION|DROP|DELETE|INSERT|UPDATE|EXEC|CREATE|ALTER|TRUNCATE|MERGE)\\b";
        return Pattern.compile(regex).matcher(upperQuery).find();
    }

    private boolean hasBalancedQuotes(String query) {
        int singleQuotes = 0;
        boolean inQuote = false;

        for (char c : query.toCharArray()) {
            if (c == '\'') {
                inQuote = !inQuote;
                singleQuotes++;
            }
        }

        return singleQuotes % 2 == 0 && !inQuote;
    }
}