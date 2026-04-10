package com.exam.broker_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_retry_jobs")
public class OrderRetryJob extends BaseRetryJob {
    
    public OrderRetryJob() {}

    public OrderRetryJob(String payload, Integer retryCount, LocalDateTime nextRetryTime, String status, String emailStatus, String updateStatus) {
        this.setPayload(payload);
        this.setRetryCount(retryCount);
        this.setNextRetryTime(nextRetryTime);
        this.setStatus(status);
        this.setEmailStatus(emailStatus);
        this.setUpdateStatus(updateStatus);
    }
}
