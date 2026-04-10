package com.exam.broker_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseRetryJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String payload; // El objeto de datos (Producto, Orden o Pago)

    private Integer retryCount = 0;
    private LocalDateTime nextRetryTime = LocalDateTime.now();
    private String status = "PENDING";

    // Estados detallados por paso (JSON)
    @Column(columnDefinition = "TEXT")
    private String emailStatus = "{\"status\":\"PENDING\", \"message\":\"\"}";
    
    @Column(columnDefinition = "TEXT")
    private String updateStatus = "{\"status\":\"PENDING\", \"message\":\"\"}";

    public BaseRetryJob() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(String emailStatus) {
        this.emailStatus = emailStatus;
    }

    public String getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(String updateStatus) {
        this.updateStatus = updateStatus;
    }
}
