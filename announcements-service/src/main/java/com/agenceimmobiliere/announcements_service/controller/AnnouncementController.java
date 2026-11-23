
package com.agenceimmobiliere.announcements_service.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agenceimmobiliere.announcements_service.entity.Announcement;
import com.agenceimmobiliere.announcements_service.service.AnnouncementService;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(
            AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public List<Announcement> getAllAnnouncements() {
        return announcementService.getAllAnnouncements();
    }
    

	@GetMapping("/{id}")
	public ResponseEntity<Announcement> getAnnouncementById(
	        @PathVariable Long id) {
	
	    Optional<Announcement> announcement =
	            announcementService.getAnnouncementById(id);
	
	    return announcement
	            .map(ResponseEntity::ok)
	            .orElseGet(() -> ResponseEntity.notFound().build());
	}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Announcement createAnnouncement(
            @RequestBody Announcement announcement) {
        return announcementService.createAnnouncement(announcement);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Announcement> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody Announcement updatedAnnouncement) {

        Optional<Announcement> announcement =
                announcementService.updateAnnouncement(id, updatedAnnouncement);

        return announcement
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {

        boolean deleted = announcementService.deleteAnnouncement(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
