package com.exam.broker_service.listener;

import com.exam.broker_service.dto.RetryMessage;
import com.exam.broker_service.model.BaseRetryJob;
import com.exam.broker_service.model.OrderRetryJob;
import com.exam.broker_service.model.PaymentRetryJob;
import com.exam.broker_service.model.ProductRetryJob;
import com.exam.broker_service.model.RetryJob;
import com.exam.broker_service.repository.OrderRetryRepository;
import com.exam.broker_service.repository.PaymentRetryRepository;
import com.exam.broker_service.repository.ProductRetryRepository;
import com.exam.broker_service.repository.RetryJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RetryJobListener {
    private static final Logger log = LoggerFactory.getLogger(RetryJobListener.class);
    private final ProductRetryRepository productRepository;
    private final OrderRetryRepository orderRepository;
    private final PaymentRetryRepository paymentRepository;
    private final RetryJobRepository centralRepository;
    private final ObjectMapper objectMapper;

    public RetryJobListener(ProductRetryRepository productRepository, 
                            OrderRetryRepository orderRepository, 
                            PaymentRetryRepository paymentRepository, 
                            RetryJobRepository centralRepository, 
                            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.centralRepository = centralRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"product_retry_jobs", "order_retry_jobs", "payments_retry_jobs"}, groupId = "broker-group")
    public void listen(RetryMessage message, String topic) {
        log.info("Mensaje recibido de Kafka en tópico {}: {}", topic, message);
        try {
            String serviceName = topic.split("_")[0].toUpperCase();
            String payloadJson = objectMapper.writeValueAsString(message.getData());
            String emailStatusJson = objectMapper.writeValueAsString(message.getSendEmail());
            String updateStatusJson = objectMapper.writeValueAsString(message.getUpdateRetryJobs());
            
            BaseRetryJob specificJob = null;
            LocalDateTime now = LocalDateTime.now();

            // 1. Persistencia en la tabla específica
            if (topic.equals("product_retry_jobs")) {
                specificJob = productRepository.save(new ProductRetryJob(payloadJson, 0, now, "PENDING", emailStatusJson, updateStatusJson));
            } else if (topic.equals("order_retry_jobs")) {
                specificJob = orderRepository.save(new OrderRetryJob(payloadJson, 0, now, "PENDING", emailStatusJson, updateStatusJson));
            } else if (topic.equals("payments_retry_jobs")) {
                specificJob = paymentRepository.save(new PaymentRetryJob(payloadJson, 0, now, "PENDING", emailStatusJson, updateStatusJson));
            }

            // 2. Registro centralizado en retry_jobs
            if (specificJob != null) {
                RetryJob centralJob = new RetryJob();
                centralJob.setEntityType(serviceName);
                centralJob.setEntitySpecificId(specificJob.getId());
                centralJob.setPayload(payloadJson);
                centralJob.setRetryCount(0);
                centralJob.setStatus("PENDING");
                centralJob.setCreatedAt(now);
                centralJob.setUpdatedAt(now);
                centralJob.setStepStatus("{\"execution\":\"PENDING\", \"email\":\"PENDING\", \"update\":\"PENDING\"}");
                
                centralRepository.save(centralJob);
                log.info("Job centralizado persistido en 'retry_jobs' para el servicio: {}", serviceName);
            }

        } catch (Exception e) {
            log.error("Error al procesar mensaje de Kafka y centralizar en 'retry_jobs': {}", e.getMessage());
        }
    }
}
