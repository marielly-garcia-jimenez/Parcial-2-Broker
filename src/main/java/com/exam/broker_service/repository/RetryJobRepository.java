package com.exam.broker_service.repository;

import com.exam.broker_service.model.RetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RetryJobRepository extends JpaRepository<RetryJob, Long> {
    Optional<RetryJob> findByEntityTypeAndEntitySpecificId(String entityType, Long specificId);
}
