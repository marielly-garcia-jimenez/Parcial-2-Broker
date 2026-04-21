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
        List<ProductRetryJob> productJobs = productRepository.findByStatusAndNextRetryTimeBefore("SCHEDULED", now);
        for (ProductRetryJob job : productJobs) {
            job.setStatus("PROCESSING");
            productRepository.save(job);
            RetryContext context = RetryContext.builder().job(job).serviceName("product").success(false).build();
            executionHandler.handle(context);
        }

        // Procesar Ordenes
        List<OrderRetryJob> orderJobs = orderRepository.findByStatusAndNextRetryTimeBefore("SCHEDULED", now);
        for (OrderRetryJob job : orderJobs) {
            job.setStatus("PROCESSING");
            orderRepository.save(job);
            RetryContext context = RetryContext.builder().job(job).serviceName("order").success(false).build();
            executionHandler.handle(context);
        }

        // Procesar Pagos
        List<PaymentRetryJob> paymentJobs = paymentRepository.findByStatusAndNextRetryTimeBefore("SCHEDULED", now);
        for (PaymentRetryJob job : paymentJobs) {
            job.setStatus("PROCESSING");
            paymentRepository.save(job);
            RetryContext context = RetryContext.builder().job(job).serviceName("payments").success(false).build();
            executionHandler.handle(context);
        }
    }
}
