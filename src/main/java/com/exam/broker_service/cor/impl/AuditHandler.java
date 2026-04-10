package com.exam.broker_service.cor.impl;

import com.exam.broker_service.cor.RetryContext;
import com.exam.broker_service.cor.RetryHandler;
import com.exam.broker_service.model.OrderRetryJob;
import com.exam.broker_service.model.PaymentRetryJob;
import com.exam.broker_service.model.ProductRetryJob;
import com.exam.broker_service.repository.OrderRetryRepository;
import com.exam.broker_service.repository.PaymentRetryRepository;
import com.exam.broker_service.repository.ProductRetryRepository;
import com.exam.broker_service.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditHandler implements RetryHandler {
    private static final Logger log = LoggerFactory.getLogger(AuditHandler.class);
    private final ProductRetryRepository productRepository;
    private final OrderRetryRepository orderRepository;
    private final PaymentRetryRepository paymentRepository;
    private final RetryJobRepository centralRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public AuditHandler(ProductRetryRepository productRepository, 
                        OrderRetryRepository orderRepository, 
                        PaymentRetryRepository paymentRepository, 
                        RetryJobRepository centralRepository, 
                        MongoTemplate mongoTemplate, 
                        ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.centralRepository = centralRepository;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setNext(RetryHandler next) {
        // Final step
    }

    @Override
    public void handle(RetryContext context) {
        log.info("PASO C: Actualizando estado final y auditoría centralizada");
        String message = context.isSuccess() ? "Éxito" : "Fallo";
        String statusStr = context.isSuccess() ? "SUCCESS" : "FAILED";
        LocalDateTime now = LocalDateTime.now();
        
        try {
            context.getJob().setUpdateStatus("{\"status\":\"" + statusStr + "\", \"message\":\"" + message + "\"}");
            
            if (context.isSuccess()) {
                context.getJob().setStatus("SUCCESS");
                Object payload = objectMapper.readValue(context.getJob().getPayload(), Object.class);
                mongoTemplate.save(payload, "recovered_data_" + context.getServiceName());
            } else {
                int retryCount = context.getJob().getRetryCount() + 1;
                context.getJob().setRetryCount(retryCount);
                long delay = 10L * (long) Math.pow(2, retryCount);
                context.getJob().setNextRetryTime(now.plusSeconds(delay));
                
                if (retryCount >= 5) {
                    context.getJob().setStatus("FAILED");
                }
            }
            
            // 1. Persistencia en la tabla específica
            String service = context.getServiceName();
            if (service.equalsIgnoreCase("product")) {
                productRepository.save((ProductRetryJob) context.getJob());
            } else if (service.equalsIgnoreCase("order")) {
                orderRepository.save((OrderRetryJob) context.getJob());
            } else if (service.equalsIgnoreCase("payments")) {
                paymentRepository.save((PaymentRetryJob) context.getJob());
            }
            
            // 2. Sincronización con la tabla central 'retry_jobs'
            centralRepository.findByEntityTypeAndEntitySpecificId(service.toUpperCase(), context.getJob().getId())
                .ifPresent(centralJob -> {
                    centralJob.setStatus(context.getJob().getStatus());
                    centralJob.setRetryCount(context.getJob().getRetryCount());
                    centralJob.setUpdatedAt(now);
                    centralJob.setStepStatus(String.format("{\"execution\":\"%s\", \"email\":\"%s\", \"update\":\"%s\"}", 
                        statusStr, context.isSuccess() ? "SUCCESS" : "FAILED", statusStr));
                    centralRepository.save(centralJob);
                    log.info("Tabla central 'retry_jobs' actualizada para el job ID: {}", centralJob.getId());
                });

            log.info("Auditoría completada ({}) para el servicio {}", statusStr, service);
        } catch (Exception e) {
            log.error("Falla crítica en PASO C y sincronización central: {}", e.getMessage());
        }
    }
}
