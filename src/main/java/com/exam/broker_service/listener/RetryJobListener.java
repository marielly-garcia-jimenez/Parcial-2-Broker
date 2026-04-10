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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryJobListener {

    private final ProductRetryRepository productRepository;
    private final OrderRetryRepository orderRepository;
    private final PaymentRetryRepository paymentRepository;
    private final RetryJobRepository centralRepository;
    private final ObjectMapper objectMapper;

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
                specificJob = productRepository.save(ProductRetryJob.builder()
                        .payload(payloadJson)
                        .retryCount(0)
                        .nextRetryTime(now)
                        .status("PENDING")
                        .emailStatus(emailStatusJson)
                        .updateStatus(updateStatusJson)
                        .build());
            } else if (topic.equals("order_retry_jobs")) {
                specificJob = orderRepository.save(OrderRetryJob.builder()
                        .payload(payloadJson)
                        .retryCount(0)
                        .nextRetryTime(now)
                        .status("PENDING")
                        .emailStatus(emailStatusJson)
                        .updateStatus(updateStatusJson)
                        .build());
            } else if (topic.equals("payments_retry_jobs")) {
                specificJob = paymentRepository.save(PaymentRetryJob.builder()
                        .payload(payloadJson)
                        .retryCount(0)
                        .nextRetryTime(now)
                        .status("PENDING")
                        .emailStatus(emailStatusJson)
                        .updateStatus(updateStatusJson)
                        .build());
            }

            // 2. Registro centralizado en retry_jobs
            if (specificJob != null) {
                RetryJob centralJob = RetryJob.builder()
                        .entityType(serviceName)
                        .entitySpecificId(specificJob.getId())
                        .payload(payloadJson)
                        .retryCount(0)
                        .status("PENDING")
                        .createdAt(now)
                        .updatedAt(now)
                        .stepStatus("{\"execution\":\"PENDING\", \"email\":\"PENDING\", \"update\":\"PENDING\"}")
                        .build();
                centralRepository.save(centralJob);
                log.info("Job centralizado persistido en 'retry_jobs' para el servicio: {}", serviceName);
            }

        } catch (Exception e) {
            log.error("Error al procesar mensaje de Kafka y centralizar en 'retry_jobs': {}", e.getMessage());
        }
    }
}
