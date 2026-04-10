package com.exam.broker_service.repository;

import com.exam.broker_service.model.ProductRetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductRetryRepository extends JpaRepository<ProductRetryJob, Long> {
    List<ProductRetryJob> findByStatusAndNextRetryTimeBefore(String status, LocalDateTime time);
}
