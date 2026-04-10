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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditHandler implements RetryHandler {
    private static final Logger log = LoggerFactory.getLogger(AuditHandler.class);
    private final ProductRetryRepository productRepository;
    private final OrderRetryRepository orderRepository;
    private final PaymentRetryRepository paymentRepository;
    private final RetryJobRepository centralRepository;

    public AuditHandler(ProductRetryRepository productRepository, 
                        OrderRetryRepository orderRepository, 
                        PaymentRetryRepository paymentRepository, 
                        RetryJobRepository centralRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.centralRepository = centralRepository;
    }

    @Override
    public void setNext(RetryHandler next) {
        // Ultimo eslabon
    }

    @Override
    public void handle(RetryContext context) {
        log.info("PASO D: Auditando resultado final en PostgreSQL");
        String statusStr = context.isSuccess() ? "SUCCESS" : "FAILED";
        LocalDateTime now = LocalDateTime.now();
        
        try {
            if (context.isSuccess()) {
                context.getJob().setStatus("SUCCESS");
            } else {
                int retryCount = context.getJob().getRetryCount() + 1;
                context.getJob().setRetryCount(retryCount);
                // Formula Prompt: NOW() + (10s * 2^retryCount)
                long delay = 10L * (long) Math.pow(2, retryCount);
                context.getJob().setNextRetryTime(now.plusSeconds(delay));
                
                if (retryCount >= 5) {
                    context.getJob().setStatus("FAILED");
                }
            }
            
            context.getJob().setUpdateStatus("{\"status\":\"" + statusStr + "\", \"message\":\"Paso D completado\"}");

            // 1. Persistencia en tabla especifica
            String service = context.getServiceName();
            if (service.equalsIgnoreCase("product")) {
                productRepository.save((ProductRetryJob) context.getJob());
            } else if (service.equalsIgnoreCase("order")) {
                orderRepository.save((OrderRetryJob) context.getJob());
            } else if (service.equalsIgnoreCase("payments")) {
                paymentRepository.save((PaymentRetryJob) context.getJob());
            }
            
            // 2. Sincronización con tabla maestra
            centralRepository.findByEntityTypeAndEntitySpecificId(service.toUpperCase(), context.getJob().getId())
                .ifPresent(centralJob -> {
                    centralJob.setStatus(context.getJob().getStatus());
                    centralJob.setRetryCount(context.getJob().getRetryCount());
                    centralJob.setUpdatedAt(now);
                    centralJob.setStepStatus(String.format("{\"A\":\"%s\", \"B\":\"SUCCESS\", \"C\":\"%s\", \"D\":\"SUCCESS\"}", 
                        statusStr, context.isSuccess() ? "SUCCESS" : "SKIPPED"));
                    centralRepository.save(centralJob);
                });

            log.info("PASO D: Auditoria finalizada ({})", statusStr);
        } catch (Exception e) {
            log.error("Falla critica en PASO D: {}", e.getMessage());
        }
    }
}
