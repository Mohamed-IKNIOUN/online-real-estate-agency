
package com.agenceimmobiliere.announcements_service.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.agenceimmobiliere.announcements_service.service.AnnouncementImageService;
import com.agenceimmobiliere.announcements_service.service.AnnouncementService;

import java.util.Objects;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AnnouncementImageService imageService;

    public AnnouncementController(
            AnnouncementService announcementService,
            AnnouncementImageService imageService) {
        this.announcementService = announcementService;
        this.imageService = imageService;
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

	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Announcement createAnnouncement(
	        @RequestBody Announcement announcement,
	        Authentication authentication) {

	    Long userId = (Long) authentication.getPrincipal();
	    announcement.setAgentId(userId);

	    return announcementService.createAnnouncement(announcement);
	}
    
	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public ResponseEntity<Announcement> updateAnnouncement(
	        @PathVariable Long id,
	        @RequestBody Announcement updatedAnnouncement,
	        Authentication authentication) {

	    Long userId = (Long) authentication.getPrincipal();

	    Optional<Announcement> existingAnnouncement =
	            announcementService.getAnnouncementById(id);

	    if (existingAnnouncement.isEmpty()) {
	        return ResponseEntity.notFound().build();
	    }

	    Announcement announcement = existingAnnouncement.get();

	    boolean isAdmin = authentication.getAuthorities().stream()
	            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

	    if (!isAdmin && !Objects.equals(announcement.getAgentId(), userId)) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	    }

	    Optional<Announcement> updated =
	            announcementService.updateAnnouncement(id, updatedAnnouncement);

	    return updated.map(ResponseEntity::ok)
	            .orElseGet(() -> ResponseEntity.notFound().build());
	}
    
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public ResponseEntity<Void> deleteAnnouncement(
	        @PathVariable Long id,
	        Authentication authentication) {

	    Long userId = (Long) authentication.getPrincipal();

	    Optional<Announcement> existingAnnouncement =
	            announcementService.getAnnouncementById(id);

	    if (existingAnnouncement.isEmpty()) {
	        return ResponseEntity.notFound().build();
	    }

	    Announcement announcement = existingAnnouncement.get();

	    boolean isAdmin = authentication.getAuthorities().stream()
	            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

	    if (!isAdmin && !Objects.equals(announcement.getAgentId(), userId)) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	    }

	    imageService.deleteAll(id);
	    announcementService.deleteAnnouncement(id);

	    return ResponseEntity.noContent().build();
	}
}
