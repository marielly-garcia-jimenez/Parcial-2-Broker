package com.exam.broker_service.service;

import com.exam.broker_service.model.ShippingRecord;
import com.exam.broker_service.repository.ShippingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShippingScheduler {
    private static final Logger log = LoggerFactory.getLogger(ShippingScheduler.class);
    private final ShippingRepository shippingRepository;
    private final JavaMailSender mailSender;

    public ShippingScheduler(ShippingRepository shippingRepository, JavaMailSender mailSender) {
        this.shippingRepository = shippingRepository;
        this.mailSender = mailSender;
    }

    @Scheduled(fixedRate = 10000)
    public void processShippings() {
        // Paso 3 Diagrama PAGOS RECIBIDOS: Tomar registros de tabla de envios
        List<ShippingRecord> pending = shippingRepository.findByStatus("PENDING_SHIPMENT");
        
        for (ShippingRecord record : pending) {
            log.info("Procesando envío para orden: {}", record.getOrderId());
            
            // Paso 3 Diagrama: Enviar correo confirmado envio de orden
            sendEmail("Confirmación de Envío", "Tu orden " + record.getOrderId() + " ha sido enviada.");
            
            record.setStatus("SHIPPED");
            record.setShippedAt(LocalDateTime.now());
            shippingRepository.save(record);
            log.info("Orden {} marcada como SHIPPED", record.getOrderId());
        }
    }

    private void sendEmail(String subject, String text) {
        try {
            String sender = System.getenv("MAIL_USERNAME");
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(sender);
            mail.setTo(sender);
            mail.setSubject(subject);
            mail.setText(text);
            mailSender.send(mail);
        } catch (Exception e) {
            log.error("Error enviando correo de envio: {}", e.getMessage());
        }
    }
}
