package com.executor.server.service;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

@Service
public class QueryValidationService {

    public void validateQuery(String query){
        if (query == null || query.trim().isEmpty()) {
            throw new SecurityException("Query cannot be empty");
        }

        Statement sqlStatement;
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(query);
            if (statements.size() != 1)
                throw new SecurityException("Query should contain only one statement");

            sqlStatement = statements.getFirst();
        } catch (JSQLParserException e) {
            throw new SecurityException("Bad query", e);
        }

        if (!(sqlStatement instanceof Select || sqlStatement instanceof DescribeStatement || sqlStatement instanceof ShowStatement)){
            throw new SecurityException("Only retrieving data queries are allowed");
        }
    }
}