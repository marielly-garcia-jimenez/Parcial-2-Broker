package com.exam.broker_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "retry_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
