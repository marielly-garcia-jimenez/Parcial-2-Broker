package com.exam.broker_service.repository;

import com.exam.broker_service.model.ShippingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShippingRepository extends JpaRepository<ShippingRecord, Long> {
    List<ShippingRecord> findByStatus(String status);
}
