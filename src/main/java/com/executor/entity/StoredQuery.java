package com.executor.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class StoredQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String query;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public StoredQuery() {
        this.createdAt = LocalDateTime.now();
    }

    public StoredQuery(String query) {
        this.query = query;
        this.createdAt = LocalDateTime.now();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
