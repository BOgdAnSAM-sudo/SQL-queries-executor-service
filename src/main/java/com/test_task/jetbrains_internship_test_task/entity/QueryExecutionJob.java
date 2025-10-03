package com.test_task.jetbrains_internship_test_task.entity;
// Using JPA for persistence, even with an in-memory H2 database.
import jakarta.persistence.*;
import java.util.UUID;

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
    private UUID id;

    private Long sourceQueryId;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Lob
    private String result;

    private String errorMessage;

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}