package com.exam.broker_service.listener;

import com.exam.broker_service.feign.ProductClient;
import com.exam.broker_service.model.ShippingRecord;
import com.exam.broker_service.repository.ShippingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BusinessFlowListener {
    private static final Logger log = LoggerFactory.getLogger(BusinessFlowListener.class);
    private final ShippingRepository shippingRepository;
    private final ProductClient productClient;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    public BusinessFlowListener(ShippingRepository shippingRepository, 
                                ProductClient productClient, 
                                JavaMailSender mailSender, 
                                ObjectMapper objectMapper) {
        this.shippingRepository = shippingRepository;
        this.productClient = productClient;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order_status_changed_events", groupId = "broker-business-group")
    public void handleOrderStatusChange(String messageJson) {
        log.info("NEGOCIO: Recibido order_status_changed_events: {}", messageJson);
        try {
            Map order = objectMapper.readValue(messageJson, Map.class);
            String status = (String) order.get("estado");
            String orderId = (String) order.get("id");

            sendEmail("Actualización de tu Orden", "Tu orden " + orderId + " ha cambiado a estado: " + status);

            if ("PAGADO".equalsIgnoreCase(status)) {
                shippingRepository.save(new ShippingRecord(orderId, "PENDING_SHIPMENT"));
                log.info("Orden {} marcada para envío en PostgreSQL", orderId);
            }
        } catch (Exception e) {
            log.error("Error en flujo ACTUALIZAR ESTATUS: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "inventory_update_events", groupId = "broker-business-group")
    public void handleInventoryUpdate(String messageJson) {
        log.info("NEGOCIO: Recibido inventory_update_events: {}", messageJson);
        try {
            Map order = objectMapper.readValue(messageJson, Map.class);
            List<String> productIds = (List<String>) order.get("productoIds");
            
            if (productIds != null && !productIds.isEmpty()) {
                Map<String, Integer> itemsToUpdate = new HashMap<>();
                for (String id : productIds) {
                    itemsToUpdate.put(id, itemsToUpdate.getOrDefault(id, 0) + 1);
                }
                productClient.updateInventory(itemsToUpdate);
                log.info("Inventario actualizado para los productos de la orden {}", order.get("id"));
            }
        } catch (Exception e) {
            log.error("Error en flujo ACTUALIZAR INVENTARIO: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment_received_events", groupId = "broker-business-group")
    public void handlePaymentReceived(String messageJson) {
        log.info("NEGOCIO: Recibido payment_received_events: {}", messageJson);
        try {
            Map payment = objectMapper.readValue(messageJson, Map.class);
            String orderId = (String) payment.get("ordenId");

            sendEmail("Pago Recibido", "Hemos recibido el pago para tu orden " + orderId);

            shippingRepository.save(new ShippingRecord(orderId, "PENDING_SHIPMENT"));
            log.info("Orden {} enviada a envíos tras pago recibido", orderId);

        } catch (Exception e) {
            log.error("Error en flujo PAGOS RECIBIDOS: {}", e.getMessage());
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
            log.info("Correo enviado: {}", subject);
        } catch (Exception e) {
            log.error("Error enviando correo de negocio: {}", e.getMessage());
        }
    }
}
