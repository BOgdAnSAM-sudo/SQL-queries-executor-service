package com.test_task.jetbrains_internship_test_task.server.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class QueryValidationService {

    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE"
    );

    private static final Pattern SQL_INJECTION_PATTERN =
            Pattern.compile("([';]+|(--)+)", Pattern.CASE_INSENSITIVE);

    public void validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String upperQuery = query.toUpperCase().trim();

        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperQuery.contains(keyword + " ") || upperQuery.contains(keyword + ";")) {
                throw new SecurityException("Query contains dangerous operation: " + keyword);
            }
        }

        // Basic SQL injection prevention
        if (SQL_INJECTION_PATTERN.matcher(query).find()) {
            throw new SecurityException("Query contains potentially dangerous characters");
        }

        // Ensure it's a SELECT query
        if (!upperQuery.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
    }
}
