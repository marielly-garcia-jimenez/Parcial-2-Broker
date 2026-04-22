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
        // El retryCount en el job aún no se ha incrementado (se incrementa en el AuditHandler)
        // Pero para la lógica de notificación, chequeamos si este intento actual es el que llegaría al límite
        boolean isDefinitiveFailure = !context.isSuccess() && (context.getJob().getRetryCount() + 1) >= 5;
        
        if (context.isSuccess()) {
            log.info("PASO B: Enviando correo de ÉXITO total.");
            sendEmail(context, "¡PROCESO COMPLETADO!: " + context.getServiceName(), 
                      "Todos los pasos (A, B, C, D) se completaron con éxito para el Job ID: " + context.getJob().getId());
            context.addStepResult("B", "SUCCESS");
        } else if (isDefinitiveFailure) {
            log.warn("PASO B: Enviando correo de FALLO DEFINITIVO.");
            sendEmail(context, "¡PROCESO FALLIDO DEFINITIVAMENTE!: " + context.getServiceName(), 
                      "El proceso ha alcanzado el límite de 5 intentos y ha fallado para el Job ID: " + context.getJob().getId());
            context.addStepResult("B", "SUCCESS");
        } else {
            log.info("PASO B: Saltando notificación (aún quedan reintentos o el proceso falló sin ser definitivo).");
            context.addStepResult("B", "SKIPPED");
        }

        if (next != null) next.handle(context);
    }

    private void sendEmail(RetryContext context, String subject, String text) {
        try {
            String sender = System.getenv("MAIL_USERNAME");
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(sender);
            mail.setTo(sender);
            mail.setSubject(subject);
            mail.setText(text);
            mailSender.send(mail);
            context.getJob().setEmailStatus("{\"status\":\"SUCCESS\", \"message\":\"Notificación enviada\"}");
        } catch (Exception e) {
            log.error("Error enviando correo en Paso B: {}", e.getMessage());
            context.getJob().setEmailStatus("{\"status\":\"FAILED\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
