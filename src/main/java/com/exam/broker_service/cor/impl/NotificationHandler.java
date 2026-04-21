package com.exam.broker_service.cor.impl;

import com.exam.broker_service.cor.RetryContext;
import com.exam.broker_service.cor.RetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class NotificationHandler implements RetryHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);
    private RetryHandler next;
    private final JavaMailSender mailSender;

    public NotificationHandler(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        if (!context.isSuccess()) {
            log.info("PASO B: Saltando registro de notificación ya que el reintento falló.");
            context.addStepResult("B", "SKIPPED");
            if (next != null) next.handle(context);
            return;
        }

        log.info("PASO B: Marcando paso de notificación como exitoso (el envío real será al final)");
        context.addStepResult("B", "SUCCESS");
        context.getJob().setEmailStatus("{\"status\":\"READY\", \"message\":\"Esperando finalización de cadena\"}");
        
        if (next != null) next.handle(context);
    }
}
