package com.executor.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
public class QueryExecutionJob {
    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sourceQueryId;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Lob
    private String result;

    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Long getSourceQueryId() {
        return sourceQueryId;
    }

    public void setSourceQueryId(Long sourceQueryId) {
        this.sourceQueryId = sourceQueryId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}