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

    @KafkaListener(topics = {"product_retry_jobs", "order_retry_jobs", "payments_retry_jobs"}, groupId = "broker-retry-group")
    public void listen(String messageJson, @org.springframework.messaging.handler.annotation.Header(org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("KAFKA RETRY: Recibido en tópico {}: {}", topic, messageJson);
        
        try {
            RetryMessage message = objectMapper.readValue(messageJson, RetryMessage.class);
            String serviceName = topic.split("_")[0].toUpperCase();
            String payloadJson;
            
            Object dataObj = message.getData();
            if (dataObj != null) {
                if (dataObj instanceof java.util.Map && ((java.util.Map)dataObj).containsKey("data")) {
                    payloadJson = objectMapper.writeValueAsString(((java.util.Map)dataObj).get("data"));
                } else {
                    payloadJson = objectMapper.writeValueAsString(dataObj);
                }
            } else {
                payloadJson = "{\"error\":\"Datos nulos\"}";
            }
            
            String emailStatusJson = (message.getSendEmail() != null) ? objectMapper.writeValueAsString(message.getSendEmail()) : "{\"status\":\"PENDING\"}";
            String updateStatusJson = (message.getUpdateRetryJobs() != null) ? objectMapper.writeValueAsString(message.getUpdateRetryJobs()) : "{\"status\":\"PENDING\"}";

            BaseRetryJob specificJob = null;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime firstRetry = now.plusSeconds(10);

            if (topic.equals("product_retry_jobs")) {
                specificJob = productRepository.save(new ProductRetryJob(payloadJson, 0, firstRetry, "SCHEDULED", emailStatusJson, updateStatusJson));
            } else if (topic.equals("order_retry_jobs")) {
                specificJob = orderRepository.save(new OrderRetryJob(payloadJson, 0, firstRetry, "SCHEDULED", emailStatusJson, updateStatusJson));
            } else if (topic.equals("payments_retry_jobs")) {
                specificJob = paymentRepository.save(new PaymentRetryJob(payloadJson, 0, firstRetry, "SCHEDULED", emailStatusJson, updateStatusJson));
            }

            if (specificJob != null) {
                RetryJob centralJob = new RetryJob();
                centralJob.setEntityType(serviceName);
                centralJob.setEntitySpecificId(specificJob.getId());
                centralJob.setPayload(payloadJson);
                centralJob.setRetryCount(0);
                centralJob.setStatus("SCHEDULED");
                centralJob.setCreatedAt(now);
                centralJob.setUpdatedAt(now);
                centralJob.setStepStatus("{\"execution\":\"SCHEDULED\", \"email\":\"SCHEDULED\", \"update\":\"SCHEDULED\"}");
                centralRepository.save(centralJob);
            }
        } catch (Exception e) {
            log.error("Error procesando reintento: {}", e.getMessage());
        }
    }
}
