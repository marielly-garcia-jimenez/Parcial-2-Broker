package com.exam.broker_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
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
}
