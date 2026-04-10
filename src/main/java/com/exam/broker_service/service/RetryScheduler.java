package com.exam.broker_service.service;

import com.exam.broker_service.cor.RetryContext;
import com.exam.broker_service.cor.impl.AuditHandler;
import com.exam.broker_service.cor.impl.ExecutionHandler;
import com.exam.broker_service.cor.impl.NotificationHandler;
import com.exam.broker_service.model.OrderRetryJob;
import com.exam.broker_service.model.PaymentRetryJob;
import com.exam.broker_service.model.ProductRetryJob;
import com.exam.broker_service.repository.OrderRetryRepository;
import com.exam.broker_service.repository.PaymentRetryRepository;
import com.exam.broker_service.repository.ProductRetryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryScheduler {

    private final ProductRetryRepository productRepository;
    private final OrderRetryRepository orderRepository;
    private final PaymentRetryRepository paymentRepository;
    private final ExecutionHandler executionHandler;
    private final NotificationHandler notificationHandler;
    private final AuditHandler auditHandler;

    @PostConstruct
    public void init() {
        executionHandler.setNext(notificationHandler);
        notificationHandler.setNext(auditHandler);
    }

    @Scheduled(fixedRate = 10000)
    public void processRetries() {
        LocalDateTime now = LocalDateTime.now();
        
        // Procesar Productos
        List<ProductRetryJob> productJobs = productRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (ProductRetryJob job : productJobs) {
            executionHandler.handle(RetryContext.builder().job(job).serviceName("product").success(false).build());
        }

        // Procesar Ordenes
        List<OrderRetryJob> orderJobs = orderRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (OrderRetryJob job : orderJobs) {
            executionHandler.handle(RetryContext.builder().job(job).serviceName("order").success(false).build());
        }

        // Procesar Pagos
        List<PaymentRetryJob> paymentJobs = paymentRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (PaymentRetryJob job : paymentJobs) {
            executionHandler.handle(RetryContext.builder().job(job).serviceName("payments").success(false).build());
        }
    }
}
