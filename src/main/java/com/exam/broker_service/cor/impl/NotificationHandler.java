package com.exam.broker_service.cor.impl;

import com.exam.broker_service.cor.RetryContext;
import com.exam.broker_service.cor.RetryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHandler implements RetryHandler {
    private RetryHandler next;
    private final JavaMailSender mailSender;

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    @Override
    public void handle(RetryContext context) {
        log.info("PASO B: Enviando correo de notificación");
        String message = context.isSuccess() ? "Éxito en reintento" : "Fallo en reintento";
        String status = context.isSuccess() ? "SUCCESS" : "FAILED";
        
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo("admin@example.com");
            mail.setSubject("Resumen de Reintento: " + context.getServiceName());
            mail.setText("El job con ID " + context.getJob().getId() + " resultó en: " + message);
            
            // mailSender.send(mail); // Simulado
            
            context.getJob().setEmailStatus("{\"status\":\"" + status + "\", \"message\":\"" + message + "\"}");
            log.info("PASO B: Correo enviado ({})", status);
            
            if (next != null) next.handle(context);
        } catch (Exception e) {
            log.error("Falla en PASO B: {}", e.getMessage());
            context.getJob().setEmailStatus("{\"status\":\"FAILED\", \"message\":\"" + e.getMessage() + "\"}");
            if (next != null) next.handle(context);
        }
    }
}
