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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import com.exam.broker_service.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditHandler implements RetryHandler {
    private static final Logger log = LoggerFactory.getLogger(AuditHandler.class);
    private final ProductRetryRepository productRepository;
    private final OrderRetryRepository orderRepository;
    private final PaymentRetryRepository paymentRepository;
    private final RetryJobRepository centralRepository;
    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;

    public AuditHandler(ProductRetryRepository productRepository, 
                        OrderRetryRepository orderRepository, 
                        PaymentRetryRepository paymentRepository, 
                        RetryJobRepository centralRepository,
                        ObjectMapper objectMapper,
                        JavaMailSender mailSender) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.centralRepository = centralRepository;
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
    }

    @Override
    public void setNext(RetryHandler next) {
        // Ultimo eslabon
    }

    @Override
    public void handle(RetryContext context) {
        log.info("PASO D: Auditando resultado final en PostgreSQL");
        context.addStepResult("D", "SUCCESS");
        String statusStr = context.isSuccess() ? "SUCCESS" : "FAILED";
        LocalDateTime now = LocalDateTime.now();
        
        try {
            if (context.isSuccess()) {
                context.getJob().setStatus("SUCCESS");
                context.getJob().setNextRetryTime(null);
            } else {
                int nextRetryCount = context.getJob().getRetryCount() + 1;
                context.getJob().setRetryCount(nextRetryCount);
                
                if (nextRetryCount >= 5) {
                    context.getJob().setStatus("FAILED");
                    context.getJob().setNextRetryTime(null); // Detener para siempre
                    log.error("¡LÍMITE ALCANZADO!: El Job de {} ha fallado 5 veces. Se detiene el proceso.", context.getServiceName());
                } else {
                    context.getJob().setStatus("SCHEDULED");
                    long delay = 10L * (long) Math.pow(2, nextRetryCount);
                    context.getJob().setNextRetryTime(now.plusSeconds(delay));
                    log.info("Reintento #{} programado para: {}", nextRetryCount, context.getJob().getNextRetryTime());
                }
            }
            
            // SIEMPRE generamos el JSON detallado aquí
            java.util.Map<String, Object> finalStatusMap = new java.util.HashMap<>();
            finalStatusMap.put("status", context.getJob().getStatus());
            finalStatusMap.put("retryCount", context.getJob().getRetryCount());
            finalStatusMap.put("steps", context.getStepResults());
            
            context.getJob().setUpdateStatus(objectMapper.writeValueAsString(finalStatusMap));

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
                    try {
                        centralJob.setStepStatus(objectMapper.writeValueAsString(context.getStepResults()));
                    } catch (Exception e) {
                        centralJob.setStepStatus("{\"error\":\"Could not serialize step results\"}");
                    }
                    centralRepository.save(centralJob);
                });

            log.info("PASO D: Auditoria finalizada ({})", statusStr);
        } catch (Exception e) {
            log.error("Falla critica en PASO D: {}", e.getMessage());
            context.addStepResult("D", "FAILED");
        }
    }
}
