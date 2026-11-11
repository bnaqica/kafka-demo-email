package com.kafkademo.email.api;

import static com.kafkademo.email.EventFixtures.CUSTOMER_ID;
import static com.kafkademo.email.EventFixtures.EVENT_ID;
import static com.kafkademo.email.TestTokens.admin;
import static com.kafkademo.email.TestTokens.viewer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kafkademo.email.config.SecurityConfig;
import com.kafkademo.email.dto.response.IncomingEventResponse;
import com.kafkademo.email.entity.IncomingEventStatus;
import com.kafkademo.email.exception.DuplicateIncomingEventException;
import com.kafkademo.email.exception.IncomingEventNotFoundException;
import com.kafkademo.email.service.IncomingEventService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncomingEventController.class)
@Import(SecurityConfig.class)
class IncomingEventControllerTest {

    private static final String PATH = "/api/emails/incoming-events";
    private static final String BODY = """
            {"eventId":"%s","eventType":"customer-addition","customerId":"%s","payload":"{}","status":"RECEIVED"}
            """.formatted(EVENT_ID, CUSTOMER_ID);
    private static final IncomingEventResponse RESPONSE = IncomingEventResponse.builder()
            .eventId(EVENT_ID)
            .eventType("customer-addition")
            .status(IncomingEventStatus.RECEIVED)
            .build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncomingEventService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createReturns201() throws Exception {
        when(service.create(any())).thenReturn(RESPONSE);

        mockMvc.perform(post(PATH)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()));
    }

    @Test
    void createExistingEventReturns409() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateIncomingEventException(EVENT_ID));

        mockMvc.perform(post(PATH)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void createRejectsMissingFields() throws Exception {
        mockMvc.perform(post(PATH)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsRecords() throws Exception {
        when(service.findAll()).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get(PATH).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));
    }

    @Test
    void getsARecord() throws Exception {
        when(service.get(EVENT_ID)).thenReturn(RESPONSE);

        mockMvc.perform(get(PATH + "/" + EVENT_ID).with(admin())).andExpect(status().isOk());
    }

    @Test
    void getUnknownRecordReturns404() throws Exception {
        when(service.get(EVENT_ID)).thenThrow(new IncomingEventNotFoundException(EVENT_ID));

        mockMvc.perform(get(PATH + "/" + EVENT_ID).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void updatesARecord() throws Exception {
        when(service.update(eq(EVENT_ID), any())).thenReturn(RESPONSE);

        mockMvc.perform(put(PATH + "/" + EVENT_ID)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete(PATH + "/" + EVENT_ID).with(admin())).andExpect(status().isNoContent());

        verify(service).delete(EVENT_ID);
    }

    @Test
    void deleteUnknownRecordReturns404() throws Exception {
        doThrow(new IncomingEventNotFoundException(EVENT_ID)).when(service).delete(EVENT_ID);

        mockMvc.perform(delete(PATH + "/" + EVENT_ID).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void viewerCannotRead() throws Exception {
        mockMvc.perform(get(PATH).with(viewer())).andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotDelete() throws Exception {
        mockMvc.perform(delete(PATH + "/" + EVENT_ID).with(viewer())).andExpect(status().isForbidden());
    }
}
