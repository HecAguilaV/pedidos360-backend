package com.pedidos360;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.mail.username=",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class NotificationsServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldSendCustomEmailViaFormEndpointAndStripSurroundingQuotes() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("to", "\"correo-del-profesor@duocuc.cl\"")
                        .param("subject", "\"Prueba en Vivo Evaluacion 2 - Pedidos360\"")
                        .param("body", "\"Demostración exitosa: Notificación enviada.\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.destinatario").value("correo-del-profesor@duocuc.cl"))
                .andExpect(jsonPath("$.asunto").value("Prueba en Vivo Evaluacion 2 - Pedidos360"))
                .andExpect(jsonPath("$.mensaje").value("¡Correo enviado exitosamente a correo-del-profesor@duocuc.cl!"));
    }

    @Test
    void shouldReturnBadRequestWhenRecipientIsBlank() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("to", "   ")
                        .param("subject", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito").value(false));
    }

    @Test
    void shouldKeepDirectJsonNotificationEndpointWorking() throws Exception {
        mockMvc.perform(post("/api/v1/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"cliente@pedidos360.cl\",\"asunto\":\"Boleta Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.destinatario").value("cliente@pedidos360.cl"));
    }

    @Test
    void shouldSafelyHandleHtmlSpecialCharactersInCustomEmailEndpoint() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("to", "profesor@duocuc.cl")
                        .param("subject", "Alerta <Stock & Pagos>")
                        .param("body", "Monto > $1000 & <script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.asunto").value("Alerta <Stock & Pagos>"));
    }
}

