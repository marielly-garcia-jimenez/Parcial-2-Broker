package com.exam.broker_service.repository;

import com.exam.broker_service.model.PaymentRetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRetryRepository extends JpaRepository<PaymentRetryJob, Long> {
    List<PaymentRetryJob> findByStatusAndNextRetryTimeBefore(String status, LocalDateTime time);
}
