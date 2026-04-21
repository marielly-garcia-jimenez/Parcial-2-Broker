package com.exam.broker_service.cor.impl;

import com.exam.broker_service.cor.RetryContext;
import com.exam.broker_service.cor.RetryHandler;
import com.exam.broker_service.feign.OrderClient;
import com.exam.broker_service.feign.PaymentClient;
import com.exam.broker_service.feign.ProductClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExecutionHandler implements RetryHandler {
    private static final Logger log = LoggerFactory.getLogger(ExecutionHandler.class);
    private RetryHandler next;
    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final PaymentClient paymentClient;
    private final ObjectMapper objectMapper;

    public ExecutionHandler(ProductClient productClient, OrderClient orderClient, PaymentClient paymentClient, ObjectMapper objectMapper) {
        this.productClient = productClient;
        this.orderClient = orderClient;
        this.paymentClient = paymentClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.info("PASO A: Reintentando ejecución para el servicio: {}", context.getServiceName());
        try {
            Object payload = objectMapper.readValue(context.getJob().getPayload(), Object.class);
            String service = context.getServiceName();
            
            if (service.equalsIgnoreCase("product")) {
                productClient.retrySave(payload);
            } else if (service.equalsIgnoreCase("order")) {
                orderClient.retrySave(payload);
            } else if (service.equalsIgnoreCase("payments")) {
                paymentClient.retrySave(payload);
            }
            
            context.setSuccess(true);
            context.addStepResult("A", "SUCCESS");
            log.info("Ejecución exitosa (PASO A) para el servicio: {}", service);
            if (next != null) next.handle(context);
        } catch (Exception e) {
            log.error("Falla en PASO A para el servicio {}: {}", context.getServiceName(), e.getMessage());
            context.setSuccess(false);
            context.addStepResult("A", "FAILED");
            if (next != null) next.handle(context);
        }
    }
}
