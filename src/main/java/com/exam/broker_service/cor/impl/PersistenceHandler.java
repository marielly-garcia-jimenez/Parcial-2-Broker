package com.exam.broker_service.cor.impl;

import com.exam.broker_service.cor.RetryContext;
import com.exam.broker_service.cor.RetryHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class PersistenceHandler implements RetryHandler {
    private static final Logger log = LoggerFactory.getLogger(PersistenceHandler.class);
    private RetryHandler next;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public PersistenceHandler(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        if (!context.isSuccess()) {
            log.info("PASO C: Saltando persistencia en MongoDB debido a que el reintento falló.");
            context.getJob().setUpdateStatus("{\"status\":\"SKIPPED\", \"message\":\"Reintento previo falló\"}");
            context.addStepResult("C", "SKIPPED");
            if (next != null) next.handle(context);
            return;
        }

        log.info("PASO C: Persistiendo datos exitosos en MongoDB");
        try {
            Object payload = objectMapper.readValue(context.getJob().getPayload(), Object.class);
            mongoTemplate.save(payload, "recovered_data_" + context.getServiceName());
            log.info("PASO C: Datos guardados en colección 'recovered_data_{}'", context.getServiceName());
            context.getJob().setUpdateStatus("{\"status\":\"SUCCESS\", \"message\":\"Datos persistidos en MongoDB\"}");
            context.addStepResult("C", "SUCCESS");
            if (next != null) next.handle(context);
        } catch (Exception e) {
            log.error("Falla en PASO C (MongoDB): {}", e.getMessage());
            context.getJob().setUpdateStatus("{\"status\":\"FAILED\", \"message\":\"" + e.getMessage() + "\"}");
            context.addStepResult("C", "FAILED");
            if (next != null) next.handle(context);
        }
    }
}
