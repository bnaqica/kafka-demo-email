package com.kafkademo.email.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kafkademo.email.config.SecurityConfig;
import com.kafkademo.email.dto.response.EmailResponse;
import com.kafkademo.email.entity.EmailStatus;
import com.kafkademo.email.exception.EmailNotFoundException;
import com.kafkademo.email.service.EmailService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmailController.class)
@Import(SecurityConfig.class)
class EmailControllerTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SOURCE_EVENT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String BODY = """
            {"customerId":"%s","from":"no-reply@test.com","to":"ada@example.com",
             "subject":"Welcome","body":"Hi","status":"SENT"}
            """.formatted(CUSTOMER_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listsEmails() throws Exception {
        when(emailService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/emails").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID.toString()))
                .andExpect(jsonPath("$[0].sourceEventId").value(SOURCE_EVENT_ID.toString()))
                .andExpect(jsonPath("$[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$[0].from").value("no-reply@test.com"))
                .andExpect(jsonPath("$[0].to").value("ada@example.com"))
                .andExpect(jsonPath("$[0].subject").value("Welcome"))
                .andExpect(jsonPath("$[0].body").value("Hi"))
                .andExpect(jsonPath("$[0].status").value("SENT"))
                .andExpect(jsonPath("$[0].sentAt").value("2026-01-01T10:00:00Z"));
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/emails")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsEmailsForCustomer() throws Exception {
        when(emailService.findByCustomer(CUSTOMER_ID)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/emails/customer/{id}", CUSTOMER_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID.toString()));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(emailService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/emails")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/emails/" + ID));
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/emails")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownEmailReturns404() throws Exception {
        when(emailService.get(ID)).thenThrow(new EmailNotFoundException(ID));

        mockMvc.perform(get("/api/emails/" + ID).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsEmail() throws Exception {
        when(emailService.update(eq(ID), any())).thenReturn(response());

        mockMvc.perform(put("/api/emails/" + ID)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/emails/" + ID).with(jwt())).andExpect(status().isNoContent());

        verify(emailService).delete(ID);
    }

    private static EmailResponse response() {
        return new EmailResponse(
                ID,
                SOURCE_EVENT_ID,
                CUSTOMER_ID,
                "no-reply@test.com",
                "ada@example.com",
                "Welcome",
                "Hi",
                EmailStatus.SENT,
                Instant.parse("2026-01-01T10:00:00Z"));
    }
}
