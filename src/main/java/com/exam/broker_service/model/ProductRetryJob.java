package com.exam.broker_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_retry_jobs")
@NoArgsConstructor
public class ProductRetryJob extends BaseRetryJob {
    @Builder
    public ProductRetryJob(String payload, Integer retryCount, LocalDateTime nextRetryTime, String status, String emailStatus, String updateStatus) {
        this.setPayload(payload);
        this.setRetryCount(retryCount);
        this.setNextRetryTime(nextRetryTime);
        this.setStatus(status);
        this.setEmailStatus(emailStatus);
        this.setUpdateStatus(updateStatus);
    }
}
