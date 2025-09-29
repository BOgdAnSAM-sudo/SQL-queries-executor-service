package com.test_task.jetbrains_internship_test_task.server.service;

import com.test_task.jetbrains_internship_test_task.entity.StoredQuery;
import com.test_task.jetbrains_internship_test_task.server.repository.StoredQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class StoredQueryService {

    private final StoredQueryRepository queryRepository;

    public StoredQueryService(StoredQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public StoredQuery addQuery(String query) throws StoredQueryException {
        if (query == null) {
            throw new StoredQueryException("Query cannot be null");
        }
        StoredQuery newQuery = new StoredQuery(query);
        return queryRepository.save(newQuery);
    }

    public List<StoredQuery> getAllQueries() {
        return queryRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<StoredQuery> getQueryById(Long id) {
        return queryRepository.findById(id);
    }


}
