package com.immobiliere.visits_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.immobiliere.visits_service.dto.CreateVisitRequest;
import com.immobiliere.visits_service.dto.VisitResponse;
import com.immobiliere.visits_service.entity.VisitStatus;
import com.immobiliere.visits_service.service.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitController.class)
class VisitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private VisitService visitService;

    // 1. POST /api/visits
    @Test
    void testCreateVisit_Success() throws Exception {
        CreateVisitRequest request = new CreateVisitRequest();
        request.setAnnouncementId(1L);
        request.setClientId(5L);
        request.setDate(LocalDate.now().plusDays(2));
        request.setHeure("14:30");

        VisitResponse response = VisitResponse.builder()
                .id(1L)
                .announcementId(1L)
                .clientId(5L)
                .date(request.getDate())
                .heure("14:30")
                .status(VisitStatus.DEMANDEE)
                .build();

        when(visitService.createVisit(any(CreateVisitRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("DEMANDEE"));
    }

    // 2. GET /api/visits/client/{clientId}
    @Test
    void testGetVisitsByClient_Success() throws Exception {
        Long clientId = 5L;
        VisitResponse response = VisitResponse.builder()
                .id(1L)
                .announcementId(1L)
                .clientId(clientId)
                .date(LocalDate.now())
                .heure("10:00")
                .status(VisitStatus.DEMANDEE)
                .build();

        when(visitService.getVisitsByClient(clientId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/visits/client/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientId").value(clientId));
    }

    // 3. GET /api/visits/agent/{agentId}
    @Test
    void testGetVisitsByAgent_Success() throws Exception {
        Long agentId = 3L;
        VisitResponse response = VisitResponse.builder()
                .id(1L)
                .announcementId(1L)
                .clientId(5L)
                .agentId(agentId)
                .date(LocalDate.now())
                .heure("11:00")
                .status(VisitStatus.CONFIRMEE)
                .build();

        when(visitService.getVisitsByAgent(agentId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/visits/agent/{agentId}", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agentId").value(agentId));
    }

    // 4. GET /api/visits/announcement/{announcementId}
    @Test
    void testGetVisitsByAnnouncement_Success() throws Exception {
        Long announcementId = 10L;
        VisitResponse response = VisitResponse.builder()
                .id(1L)
                .announcementId(announcementId)
                .clientId(5L)
                .date(LocalDate.now())
                .heure("15:00")
                .status(VisitStatus.DEMANDEE)
                .build();

        when(visitService.getVisitsByAnnouncement(announcementId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/visits/announcement/{announcementId}", announcementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].announcementId").value(announcementId));
    }

    // 5. PATCH /api/visits/{id}/status
    @Test
    void testUpdateVisitStatus_Success() throws Exception {
        Long visitId = 1L;
        VisitResponse response = VisitResponse.builder()
                .id(visitId)
                .announcementId(1L)
                .clientId(5L)
                .date(LocalDate.now())
                .heure("10:00")
                .status(VisitStatus.CONFIRMEE)
                .build();

        when(visitService.updateVisitStatus(eq(visitId), eq(VisitStatus.CONFIRMEE))).thenReturn(response);

        mockMvc.perform(patch("/api/visits/{id}/status", visitId)
                        .param("status", "CONFIRMEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMEE"));
    }

    // 6. DELETE /api/visits/{id}
    @Test
    void testDeleteVisit_Success() throws Exception {
        Long visitId = 1L;

        mockMvc.perform(delete("/api/visits/{id}", visitId))
                .andExpect(status().isNoContent());
    }
}