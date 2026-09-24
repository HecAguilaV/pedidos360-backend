package com.pedidos360.service;

import com.pedidos360.event.OrdenCreadaEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOrderReceiptEmail(OrdenCreadaEvent event) {
        String recipient = (event.clienteEmail() != null && !event.clienteEmail().isBlank())
                ? event.clienteEmail()
                : "cliente@pedidos360.cl";

        String subject = "Boleta Electrónica de Compra - Pedidos360 #" + event.orderId();
        String htmlContent = buildReceiptHtml(
                event.orderId(),
                event.cliente(),
                recipient,
                event.total(),
                event.items()
        );

        return sendHtmlEmail(recipient, subject, htmlContent);
    }

    public boolean sendDirectEmail(String recipient, String subject, String clientName, Long orderId, BigDecimal total, List<OrdenCreadaEvent.ItemOrdenEvent> items) {
        String htmlContent = buildReceiptHtml(orderId, clientName, recipient, total, items);
        return sendHtmlEmail(recipient, subject, htmlContent);
    }

    private boolean sendHtmlEmail(String recipient, String subject, String htmlContent) {
        log.info("==> [EmailService] Preparando envio de correo para: '{}' | Asunto: '{}'", recipient, subject);

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("[EmailService SIMULACION] No se configuro MAIL_USERNAME en variables de entorno.");
            log.info("[EmailService SIMULACION] Correo HTML simulado enviado con exito a: {}", recipient);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Correo electronico REAL enviado exitosamente a: {}", recipient);
            return true;
        } catch (Exception e) {
            log.error("[EmailService ERROR] Fallo al enviar correo via SMTP: {}", e.getMessage());
            return false;
        }
    }

    private String buildReceiptHtml(Long orderId, String clientName, String recipient, BigDecimal total, List<OrdenCreadaEvent.ItemOrdenEvent> items) {
        StringBuilder itemsRows = new StringBuilder();
        if (items != null && !items.isEmpty()) {
            for (OrdenCreadaEvent.ItemOrdenEvent item : items) {
                BigDecimal subtotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
                itemsRows.append(String.format("""
                    <tr style="border-bottom: 1px solid #e2e8f0;">
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #1e293b;">%s</td>
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #64748b; text-align: center;">%d</td>
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #64748b; text-align: right;">$%.2f</td>
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; font-weight: bold; color: #0f172a; text-align: right;">$%.2f</td>
                    </tr>
                """, item.productName(), item.quantity(), item.unitPrice(), subtotal));
            }
        } else {
            itemsRows.append("""
                <tr>
                    <td colspan="4" style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #64748b; text-align: center;">Compra de Productos Pedidos360</td>
                </tr>
            """);
        }

        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin: 0; padding: 20px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0;">
                    <!-- Cabecera -->
                    <div style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 30px; color: #ffffff; text-align: center;">
                        <h1 style="margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">PEDIDOS 360</h1>
                        <p style="margin: 5px 0 0 0; font-size: 14px; color: #94a3b8;">Comprobante Oficial de Compra / Boleta Electrónica</p>
                    </div>

                    <!-- Datos de la orden -->
                    <div style="padding: 24px; border-bottom: 1px solid #f1f5f9;">
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr>
                                <td style="font-size: 13px; color: #64748b;">N° de Orden: <strong style="color: #0f172a;">#%s</strong></td>
                                <td style="font-size: 13px; color: #64748b; text-align: right;">Cliente: <strong style="color: #0f172a;">%s</strong></td>
                            </tr>
                            <tr>
                                <td style="font-size: 13px; color: #64748b; padding-top: 6px;">Email: <strong style="color: #0f172a;">%s</strong></td>
                                <td style="font-size: 13px; color: #64748b; padding-top: 6px; text-align: right;">Estado: <span style="background: #dcfce7; color: #15803d; padding: 2px 8px; border-radius: 9999px; font-weight: 600; font-size: 12px;">PAGADA</span></td>
                            </tr>
                        </table>
                    </div>

                    <!-- Tabla de productos -->
                    <div style="padding: 24px;">
                        <h3 style="margin: 0 0 16px 0; font-size: 16px; color: #0f172a;">Detalle de la Compra</h3>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <thead>
                                <tr style="background-color: #f8fafc; border-bottom: 2px solid #e2e8f0;">
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: left;">Producto</th>
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: center;">Cant.</th>
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: right;">Precio</th>
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: right;">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                            <tfoot>
                                <tr>
                                    <td colspan="3" style="padding: 16px 12px; font-size: 16px; font-weight: bold; color: #0f172a; text-align: right;">TOTAL:</td>
                                    <td style="padding: 16px 12px; font-size: 20px; font-weight: 800; color: #059669; text-align: right;">$%.2f</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>

                    <!-- Pie de pagina -->
                    <div style="background: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0;">
                        <p style="margin: 0; font-size: 13px; color: #64748b;">Tu pedido ya está siendo preparado por el microservicio de logística.</p>
                        <p style="margin: 6px 0 0 0; font-size: 11px; color: #94a3b8;">Evaluación Cloud Native I &bull; DSY1107 &bull; Duoc UC</p>
                    </div>
                </div>
            </body>
            </html>
        """, orderId != null ? orderId : 1001L, clientName, recipient, itemsRows.toString(), total != null ? total : BigDecimal.ZERO);
    }
}
