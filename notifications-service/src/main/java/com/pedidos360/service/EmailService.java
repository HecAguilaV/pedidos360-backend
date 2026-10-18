package com.pedidos360.service;

import com.pedidos360.event.OrdenCreadaEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Locale CHILE_LOCALE = Locale.forLanguageTag("es-CL");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOrderReceiptEmail(OrdenCreadaEvent event) {
        String recipient = (event.clienteEmail() != null && !event.clienteEmail().isBlank())
                ? sanitizeParam(event.clienteEmail())
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
        String cleanRecipient = sanitizeParam(recipient);
        String cleanSubject = sanitizeParam(subject);
        String htmlContent = buildReceiptHtml(orderId, clientName, cleanRecipient, total, items);
        return sendHtmlEmail(cleanRecipient, cleanSubject, htmlContent);
    }

    public boolean sendCustomEmail(String recipient, String subject, String body) {
        String cleanRecipient = sanitizeParam(recipient);
        String cleanSubject = sanitizeParam(subject);
        String resolvedSubject = !cleanSubject.isBlank() ? cleanSubject : "Notificación Pedidos360";
        String cleanBody = sanitizeParam(body);
        String htmlContent = buildCustomNotificationHtml(cleanRecipient, resolvedSubject, cleanBody);
        return sendHtmlEmail(cleanRecipient, resolvedSubject, htmlContent);
    }

    public String sanitizeParam(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("^\"+|\"+$", "").trim();
    }

    public static String formatClp(BigDecimal amount) {
        BigDecimal safe = amount != null ? amount.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        NumberFormat nf = NumberFormat.getIntegerInstance(CHILE_LOCALE);
        return "$" + nf.format(safe);
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

    private String buildCustomNotificationHtml(String recipient, String subject, String body) {
        String safeSubject = HtmlUtils.htmlEscape(subject != null ? subject : "");
        String safeBody = HtmlUtils.htmlEscape(body != null ? body : "");
        String safeRecipient = HtmlUtils.htmlEscape(recipient != null ? recipient : "");

        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin: 0; padding: 20px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0;">
                    <div style="background: #0f172a; padding: 28px; color: #ffffff; text-align: center;">
                        <h1 style="margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;">Pedidos360</h1>
                        <p style="margin: 4px 0 0 0; font-size: 13px; color: #94a3b8;">Notificación de Atención al Cliente</p>
                    </div>
                    <div style="padding: 24px;">
                        <h2 style="margin: 0 0 12px 0; font-size: 18px; color: #0f172a;">%s</h2>
                        <p style="margin: 0 0 16px 0; font-size: 15px; line-height: 1.6; color: #334155; white-space: pre-line;">%s</p>
                        <p style="margin: 0; font-size: 13px; color: #64748b;">Destinatario: <strong style="color: #0f172a;">%s</strong></p>
                    </div>
                </div>
            </body>
            </html>
        """, safeSubject, safeBody, safeRecipient);
    }

    private String buildReceiptHtml(Long orderId, String clientName, String recipient, BigDecimal total, List<OrdenCreadaEvent.ItemOrdenEvent> items) {
        BigDecimal grossTotal = total != null ? total.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal netAmount = grossTotal.divide(new BigDecimal("1.19"), 0, RoundingMode.HALF_UP);
        BigDecimal ivaAmount = grossTotal.subtract(netAmount);

        StringBuilder itemsRows = new StringBuilder();
        if (items != null && !items.isEmpty()) {
            for (OrdenCreadaEvent.ItemOrdenEvent item : items) {
                BigDecimal unitPrice = item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
                String safeProductName = HtmlUtils.htmlEscape(item.productName() != null ? item.productName() : "");
                itemsRows.append(String.format("""
                    <tr style="border-bottom: 1px solid #e2e8f0;">
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #1e293b;">%s</td>
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #64748b; text-align: center;">%d</td>
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #64748b; text-align: right;">%s</td>
                        <td style="padding: 12px; font-family: sans-serif; font-size: 14px; font-weight: bold; color: #0f172a; text-align: right;">%s</td>
                    </tr>
                """, safeProductName, item.quantity(), formatClp(unitPrice), formatClp(subtotal)));
            }
        } else {
            itemsRows.append("""
                <tr>
                    <td colspan="4" style="padding: 12px; font-family: sans-serif; font-size: 14px; color: #64748b; text-align: center;">Compra en Pedidos360</td>
                </tr>
            """);
        }

        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin: 0; padding: 20px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0;">
                    <div style="background: #0f172a; padding: 28px; color: #ffffff; text-align: center;">
                        <h1 style="margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;">Pedidos360</h1>
                        <p style="margin: 4px 0 0 0; font-size: 13px; color: #94a3b8;">Boleta Electrónica de Compra (CLP)</p>
                    </div>

                    <div style="padding: 24px; border-bottom: 1px solid #f1f5f9;">
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr>
                                <td style="font-size: 13px; color: #64748b;">N° de Orden: <strong style="color: #0f172a;">#%s</strong></td>
                                <td style="font-size: 13px; color: #64748b; text-align: right;">Cliente: <strong style="color: #0f172a;">%s</strong></td>
                            </tr>
                            <tr>
                                <td style="font-size: 13px; color: #64748b; padding-top: 6px;">Correo: <strong style="color: #0f172a;">%s</strong></td>
                                <td style="font-size: 13px; color: #64748b; padding-top: 6px; text-align: right;">Estado: <span style="background: #ecfdf5; color: #047857; padding: 2px 8px; border-radius: 9999px; font-weight: 600; font-size: 12px;">PAGADO</span></td>
                            </tr>
                        </table>
                    </div>

                    <div style="padding: 24px;">
                        <h3 style="margin: 0 0 16px 0; font-size: 16px; color: #0f172a;">Detalle de Productos</h3>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <thead>
                                <tr style="background-color: #f8fafc; border-bottom: 2px solid #e2e8f0;">
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: left;">Producto</th>
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: center;">Cant.</th>
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: right;">Precio Unit.</th>
                                    <th style="padding: 10px 12px; font-size: 12px; text-transform: uppercase; color: #475569; text-align: right;">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                            <tfoot>
                                <tr>
                                    <td colspan="3" style="padding: 10px 12px 4px 12px; font-size: 13px; color: #64748b; text-align: right;">Monto Neto:</td>
                                    <td style="padding: 10px 12px 4px 12px; font-size: 13px; color: #0f172a; text-align: right;">%s</td>
                                </tr>
                                <tr>
                                    <td colspan="3" style="padding: 4px 12px 10px 12px; font-size: 13px; color: #64748b; text-align: right;">IVA (19%%):</td>
                                    <td style="padding: 4px 12px 10px 12px; font-size: 13px; color: #0f172a; text-align: right;">%s</td>
                                </tr>
                                <tr style="border-top: 1px solid #e2e8f0;">
                                    <td colspan="3" style="padding: 12px; font-size: 15px; font-weight: bold; color: #0f172a; text-align: right;">TOTAL PAGADO (CLP):</td>
                                    <td style="padding: 12px; font-size: 18px; font-weight: 800; color: #0f172a; text-align: right;">%s</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>

                    <div style="background: #f8fafc; padding: 18px; text-align: center; border-top: 1px solid #e2e8f0;">
                        <p style="margin: 0; font-size: 13px; color: #64748b;">Gracias por tu compra en Pedidos360. Puedes seguir el estado de tu despacho en la sección Seguimiento.</p>
                    </div>
                </div>
            </body>
            </html>
        """,
                orderId != null ? orderId : 1001L,
                HtmlUtils.htmlEscape(clientName != null ? clientName : ""),
                HtmlUtils.htmlEscape(recipient != null ? recipient : ""),
                itemsRows.toString(),
                formatClp(netAmount),
                formatClp(ivaAmount),
                formatClp(grossTotal));
    }
}
