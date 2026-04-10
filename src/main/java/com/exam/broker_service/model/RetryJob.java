package com.exam.broker_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "retry_jobs")
public class RetryJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType; // PRODUCT, ORDER, PAYMENT
    
    private Long entitySpecificId; // ID en la tabla específica (opcional para vinculación)

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Integer retryCount;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private String status; // PENDING, SUCCESS, FAILED

    @Column(columnDefinition = "TEXT")
    private String stepStatus; // Resumen del estado actual de los pasos

    public RetryJob() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntitySpecificId() {
        return entitySpecificId;
    }

    public void setEntitySpecificId(Long entitySpecificId) {
        this.entitySpecificId = entitySpecificId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStepStatus() {
        return stepStatus;
    }

    public void setStepStatus(String stepStatus) {
        this.stepStatus = stepStatus;
    }
}
