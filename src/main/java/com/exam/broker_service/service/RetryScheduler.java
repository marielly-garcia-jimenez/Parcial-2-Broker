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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RetryScheduler {
    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);
    private final ProductRetryRepository productRepository;
    private final OrderRetryRepository orderRepository;
    private final PaymentRetryRepository paymentRepository;
    private final ExecutionHandler executionHandler;
    private final NotificationHandler notificationHandler;
    private final AuditHandler auditHandler;

    public RetryScheduler(ProductRetryRepository productRepository, 
                          OrderRetryRepository orderRepository, 
                          PaymentRetryRepository paymentRepository, 
                          ExecutionHandler executionHandler, 
                          NotificationHandler notificationHandler, 
                          AuditHandler auditHandler) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.executionHandler = executionHandler;
        this.notificationHandler = notificationHandler;
        this.auditHandler = auditHandler;
    }

    @PostConstruct
    public void init() {
        executionHandler.setNext(notificationHandler);
        notificationHandler.setNext(auditHandler);
    }

    @Scheduled(fixedRate = 10000)
    public void processRetries() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Ciclo de reintento iniciado en: {}", now);
        
        // Procesar Productos
        List<ProductRetryJob> productJobs = productRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (ProductRetryJob job : productJobs) {
            executionHandler.handle(new RetryContext(job, "product", false));
        }

        // Procesar Ordenes
        List<OrderRetryJob> orderJobs = orderRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (OrderRetryJob job : orderJobs) {
            executionHandler.handle(new RetryContext(job, "order", false));
        }

        // Procesar Pagos
        List<PaymentRetryJob> paymentJobs = paymentRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (PaymentRetryJob job : paymentJobs) {
            executionHandler.handle(new RetryContext(job, "payments", false));
        }
    }
}
