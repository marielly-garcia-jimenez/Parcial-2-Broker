package com.exam.broker_service.service;

import com.exam.broker_service.cor.RetryContext;
import com.exam.broker_service.cor.impl.AuditHandler;
import com.exam.broker_service.cor.impl.ExecutionHandler;
import com.exam.broker_service.cor.impl.NotificationHandler;
import com.exam.broker_service.cor.impl.PersistenceHandler;
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
    private final PersistenceHandler persistenceHandler;
    private final AuditHandler auditHandler;

    public RetryScheduler(ProductRetryRepository productRepository, 
                          OrderRetryRepository orderRepository, 
                          PaymentRetryRepository paymentRepository, 
                          ExecutionHandler executionHandler, 
                          NotificationHandler notificationHandler,
                          PersistenceHandler persistenceHandler,
                          AuditHandler auditHandler) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.executionHandler = executionHandler;
        this.notificationHandler = notificationHandler;
        this.persistenceHandler = persistenceHandler;
        this.auditHandler = auditHandler;
    }

    @PostConstruct
    public void init() {
        // Configurar Cadena de 4 Pasos según Prompt: A -> B -> C -> D
        executionHandler.setNext(notificationHandler);
        notificationHandler.setNext(persistenceHandler);
        persistenceHandler.setNext(auditHandler);
    }

    @Scheduled(fixedRate = 10000)
    public void processRetries() {
        LocalDateTime now = LocalDateTime.now();
        
        // Procesar Productos con chequeo de Idempotencia
        List<ProductRetryJob> productJobs = productRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (ProductRetryJob job : productJobs) {
            if ("PENDING".equals(job.getStatus())) { // Idempotencia básica
                executionHandler.handle(new RetryContext(job, "product", false));
            }
        }

        // Procesar Ordenes
        List<OrderRetryJob> orderJobs = orderRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (OrderRetryJob job : orderJobs) {
            if ("PENDING".equals(job.getStatus())) {
                executionHandler.handle(new RetryContext(job, "order", false));
            }
        }

        // Procesar Pagos
        List<PaymentRetryJob> paymentJobs = paymentRepository.findByStatusAndNextRetryTimeBefore("PENDING", now);
        for (PaymentRetryJob job : paymentJobs) {
            if ("PENDING".equals(job.getStatus())) {
                executionHandler.handle(new RetryContext(job, "payments", false));
            }
        }
    }
}
