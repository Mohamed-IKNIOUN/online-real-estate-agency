package com.immobiliere.visits_service.service;

import com.immobiliere.visits_service.dto.CreateVisitRequest;
import com.immobiliere.visits_service.dto.VisitResponse;
import com.immobiliere.visits_service.entity.Visit;
import com.immobiliere.visits_service.entity.VisitStatus;
import com.immobiliere.visits_service.exception.DuplicateVisitException;
import com.immobiliere.visits_service.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VisitServiceTest {

    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private VisitService visitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------------------------------------------
    // TESTS DE CRÉATION (CREATE)
    // ---------------------------------------------------------

    // 1. CAS NORMAL : Création réussie d'une visite
    @Test
    void testCreateVisit_NormalCase() {
        CreateVisitRequest request = new CreateVisitRequest();
        request.setAnnouncementId(1L);
        request.setClientId(5L);
        request.setDate(LocalDate.now().plusDays(3));
        request.setHeure("14:30");

        when(visitRepository.existsConflictingVisit(any(), any(), any(), any())).thenReturn(false);
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> {
            Visit v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        VisitResponse response = visitService.createVisit(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(VisitStatus.DEMANDEE, response.getStatus());
        assertEquals("14:30", response.getHeure());
        verify(visitRepository, times(1)).save(any(Visit.class));
    }

    // 2. CAS D'ERREUR : Tentative de création avec un conflit de réservation (Doublon)
    @Test
    void testCreateVisit_ErrorDuplicateConflict() {
        CreateVisitRequest request = new CreateVisitRequest();
        request.setAnnouncementId(1L);
        request.setClientId(5L);
        request.setDate(LocalDate.now().plusDays(3));
        request.setHeure("14:30");

        when(visitRepository.existsConflictingVisit(any(), any(), any(), any())).thenReturn(true);

        assertThrows(DuplicateVisitException.class, () -> {
            visitService.createVisit(request);
        });

        verify(visitRepository, never()).save(any(Visit.class));
    }

    // ---------------------------------------------------------
    // TESTS DE LECTURE (READ / GET)
    // ---------------------------------------------------------

    // 3. CAS NORMAL : Récupération d'une visite par son ID
    @Test
    void testGetVisitById_NormalCase() {
        Long visitId = 1L;
        Visit visit = Visit.builder()
                .id(visitId)
                .announcementId(1L)
                .clientId(5L)
                .date(LocalDate.now().plusDays(2))
                .heure(LocalTime.of(10, 0))
                .status(VisitStatus.DEMANDEE)
                .build();

        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        VisitResponse response = visitService.getVisitById(visitId);

        assertNotNull(response);
        assertEquals(visitId, response.getId());
        assertEquals(1L, response.getAnnouncementId());
        verify(visitRepository, times(1)).findById(visitId);
    }

    // 4. CAS D'ERREUR : Récupération d'une visite inexistante par ID
    @Test
    void testGetVisitById_ErrorNotFound() {
        Long nonExistentId = 999L;
        when(visitRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            visitService.getVisitById(nonExistentId);
        });

        verify(visitRepository, times(1)).findById(nonExistentId);
    }

    // 5. CAS NORMAL : Récupération de toutes les visites (Liste non vide)
    @Test
    void testGetAllVisits_NormalCase() {
        Visit visit1 = Visit.builder().id(1L).announcementId(1L).clientId(5L).date(LocalDate.now()).heure(LocalTime.of(9, 0)).status(VisitStatus.DEMANDEE).build();
        Visit visit2 = Visit.builder().id(2L).announcementId(2L).clientId(6L).date(LocalDate.now()).heure(LocalTime.of(11, 0)).status(VisitStatus.CONFIRMEE).build();

        when(visitRepository.findAll()).thenReturn(List.of(visit1, visit2));

        // On passe null, null pour déclencher le bloc "else" (findAll)
        List<VisitResponse> responses = visitService.getAllVisits(null, null);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(visitRepository, times(1)).findAll();
    }

    // 6. CAS LIMITE : Récupération de toutes les visites (Liste vide)
    @Test
    void testGetAllVisits_EmptyList() {
        when(visitRepository.findAll()).thenReturn(List.of());

        // On passe null, null pour déclencher le bloc "else" (findAll)
        List<VisitResponse> responses = visitService.getAllVisits(null, null);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(visitRepository, times(1)).findAll();
    }

    // 6bis. CAS FILTRE : Récupération des visites par Client ID
    @Test
    void testGetAllVisits_WithClientId() {
        Visit visit = Visit.builder().id(1L).announcementId(1L).clientId(5L).date(LocalDate.now()).heure(LocalTime.of(9, 0)).status(VisitStatus.DEMANDEE).build();

        when(visitRepository.findByClientId(5L)).thenReturn(List.of(visit));

        List<VisitResponse> responses = visitService.getAllVisits(5L, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(5L, responses.get(0).getClientId());
        verify(visitRepository, times(1)).findByClientId(5L);
        verify(visitRepository, never()).findAll();
    }

    // 6ter. CAS FILTRE : Récupération des visites par Announcement ID
    @Test
    void testGetAllVisits_WithAnnouncementId() {
        Visit visit = Visit.builder().id(1L).announcementId(1L).clientId(5L).date(LocalDate.now()).heure(LocalTime.of(9, 0)).status(VisitStatus.DEMANDEE).build();

        when(visitRepository.findByAnnouncementId(1L)).thenReturn(List.of(visit));

        List<VisitResponse> responses = visitService.getAllVisits(null, 1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getAnnouncementId());
        verify(visitRepository, times(1)).findByAnnouncementId(1L);
        verify(visitRepository, never()).findAll();
    }

    // ---------------------------------------------------------
    // TESTS DE MISE À JOUR (UPDATE)
    // ---------------------------------------------------------

    // 7. CAS NORMAL : Mise à jour valide du statut d'une visite
    @Test
    void testUpdateVisitStatus_NormalCase() {
        Long visitId = 1L;
        Visit existingVisit = Visit.builder()
                .id(visitId)
                .announcementId(1L)
                .clientId(5L)
                .date(LocalDate.now().plusDays(3))
                .heure(LocalTime.of(14, 30))
                .status(VisitStatus.DEMANDEE)
                .build();

        when(visitRepository.findById(visitId)).thenReturn(Optional.of(existingVisit));
        when(visitRepository.save(any(Visit.class))).thenReturn(existingVisit);

        VisitResponse response = visitService.updateVisitStatus(visitId, VisitStatus.CONFIRMEE);

        assertNotNull(response);
        assertEquals(VisitStatus.CONFIRMEE, response.getStatus());
        verify(visitRepository, times(1)).save(existingVisit);
    }

    // 8. CAS D'ERREUR : Tentative de mise à jour du statut d'une visite inexistante
    @Test
    void testUpdateVisitStatus_ErrorNotFound() {
        Long nonExistentId = 999L;
        when(visitRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            visitService.updateVisitStatus(nonExistentId, VisitStatus.CONFIRMEE);
        });

        verify(visitRepository, never()).save(any(Visit.class));
    }

    // ---------------------------------------------------------
    // TESTS DE SUPPRESSION (DELETE)
    // ---------------------------------------------------------

    // 9. CAS NORMAL : Suppression réussie d'une visite existante
    @Test
    void testDeleteVisit_NormalCase() {
        Long visitId = 1L;

        // Le service utilise existsById et deleteById
        when(visitRepository.existsById(visitId)).thenReturn(true);
        doNothing().when(visitRepository).deleteById(visitId);

        assertDoesNotThrow(() -> {
            visitService.deleteVisit(visitId);
        });

        verify(visitRepository, times(1)).existsById(visitId);
        verify(visitRepository, times(1)).deleteById(visitId);
    }

    // 10. CAS D'ERREUR : Suppression d'une visite inexistante
    @Test
    void testDeleteVisit_ErrorNotFound() {
        Long nonExistentId = 999L;
        when(visitRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            visitService.deleteVisit(nonExistentId);
        });

        verify(visitRepository, never()).delete(any(Visit.class));
    }
}