package com.agenceimmobiliere.announcements_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agenceimmobiliere.announcements_service.entity.Announcement;
import com.agenceimmobiliere.announcements_service.enums.PropertyType;
import com.agenceimmobiliere.announcements_service.enums.TransactionType;
import com.agenceimmobiliere.announcements_service.repository.AnnouncementRepository;
import com.agenceimmobiliere.announcements_service.service.AnnouncementService;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    // Helper method to create a test announcement
    private Announcement createTestAnnouncement() {

        Announcement announcement = new Announcement();

        announcement.setTitle("Modern apartment");
        announcement.setDescription("Beautiful apartment in Agadir");
        announcement.setPrice(new BigDecimal("750000"));
        announcement.setPropertyType(PropertyType.APARTMENT);
        announcement.setTransactionType(TransactionType.SALE);
        announcement.setCity("Agadir");
        announcement.setArea(new BigDecimal("95"));

        return announcement;
    }

    // Test 1: Get all announcements
    @Test
    void getAllAnnouncements_shouldReturnAllAnnouncements() {

        Announcement announcement = createTestAnnouncement();

        when(announcementRepository.findAll())
                .thenReturn(List.of(announcement));

        List<Announcement> result =
                announcementService.getAllAnnouncements();

        assertEquals(1, result.size());
        assertEquals("Modern apartment", result.get(0).getTitle());

        verify(announcementRepository).findAll();
    }

    // Test 2: Get announcement by existing ID
    @Test
    void getAnnouncementById_shouldReturnAnnouncementWhenFound() {

        Announcement announcement = createTestAnnouncement();

        when(announcementRepository.findById(1L))
                .thenReturn(Optional.of(announcement));

        Optional<Announcement> result =
                announcementService.getAnnouncementById(1L);

        assertTrue(result.isPresent());
        assertEquals("Modern apartment", result.get().getTitle());

        verify(announcementRepository).findById(1L);
    }

    // Test 3: Get announcement by non-existing ID
    @Test
    void getAnnouncementById_shouldReturnEmptyWhenNotFound() {

        when(announcementRepository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<Announcement> result =
                announcementService.getAnnouncementById(99L);

        assertTrue(result.isEmpty());

        verify(announcementRepository).findById(99L);
    }

    // Test 4: Create announcement
    @Test
    void createAnnouncement_shouldSaveAnnouncement() {

        Announcement announcement = createTestAnnouncement();

        when(announcementRepository.save(announcement))
                .thenReturn(announcement);

        Announcement result =
                announcementService.createAnnouncement(announcement);

        assertNotNull(result);
        assertEquals("Modern apartment", result.getTitle());

        verify(announcementRepository).save(announcement);
    }

    // Test 5: Update existing announcement
    @Test
    void updateAnnouncement_shouldUpdateAnnouncementWhenFound() {

        Announcement existingAnnouncement = createTestAnnouncement();

        Announcement updatedAnnouncement = createTestAnnouncement();
        updatedAnnouncement.setTitle("Luxury villa");
        updatedAnnouncement.setCity("Marrakech");
        updatedAnnouncement.setPrice(new BigDecimal("1200000"));

        when(announcementRepository.findById(1L))
                .thenReturn(Optional.of(existingAnnouncement));

        when(announcementRepository.save(existingAnnouncement))
                .thenReturn(existingAnnouncement);

        Optional<Announcement> result =
                announcementService.updateAnnouncement(
                        1L, updatedAnnouncement);

        assertTrue(result.isPresent());
        assertEquals("Luxury villa", result.get().getTitle());
        assertEquals("Marrakech", result.get().getCity());
        assertEquals(new BigDecimal("1200000"), result.get().getPrice());

        verify(announcementRepository).findById(1L);
        verify(announcementRepository).save(existingAnnouncement);
    }

    // Test 6: Update non-existing announcement
    @Test
    void updateAnnouncement_shouldReturnEmptyWhenNotFound() {

        Announcement updatedAnnouncement = createTestAnnouncement();

        when(announcementRepository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<Announcement> result =
                announcementService.updateAnnouncement(
                        99L, updatedAnnouncement);

        assertTrue(result.isEmpty());

        verify(announcementRepository).findById(99L);
        verify(announcementRepository, never()).save(any());
    }

    // Test 7: Delete existing announcement
    @Test
    void deleteAnnouncement_shouldReturnTrueWhenFound() {

        when(announcementRepository.existsById(1L))
                .thenReturn(true);

        boolean result = announcementService.deleteAnnouncement(1L);

        assertTrue(result);

        verify(announcementRepository).existsById(1L);
        verify(announcementRepository).deleteById(1L);
    }

    // Test 8: Delete non-existing announcement
    @Test
    void deleteAnnouncement_shouldReturnFalseWhenNotFound() {

        when(announcementRepository.existsById(99L))
                .thenReturn(false);

        boolean result = announcementService.deleteAnnouncement(99L);

        assertFalse(result);

        verify(announcementRepository).existsById(99L);
        verify(announcementRepository, never()).deleteById(anyLong());
    }
}