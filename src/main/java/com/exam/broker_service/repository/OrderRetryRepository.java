package com.exam.broker_service.repository;

import com.exam.broker_service.model.OrderRetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRetryRepository extends JpaRepository<OrderRetryJob, Long> {
    List<OrderRetryJob> findByStatusAndNextRetryTimeBefore(String status, LocalDateTime time);
}
