package com.executor.server.repository;

import com.executor.entity.StoredQuery;
import com.executor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
//    List<StoredQuery> getAllBy
}
