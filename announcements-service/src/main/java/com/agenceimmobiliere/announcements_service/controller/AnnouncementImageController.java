package com.agenceimmobiliere.announcements_service.controller;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.agenceimmobiliere.announcements_service.entity.Announcement;
import com.agenceimmobiliere.announcements_service.entity.AnnouncementImage;
import com.agenceimmobiliere.announcements_service.service.AnnouncementImageService;
import com.agenceimmobiliere.announcements_service.service.AnnouncementService;

@RestController
@RequestMapping("/api/announcements/{announcementId}/images")
public class AnnouncementImageController {

    public record ImageResponse(Long id, String url, String contentType, int position) {
    }

    private final AnnouncementService announcementService;
    private final AnnouncementImageService imageService;

    public AnnouncementImageController(
            AnnouncementService announcementService,
            AnnouncementImageService imageService) {
        this.announcementService = announcementService;
        this.imageService = imageService;
    }

    // Public: anyone browsing a listing can see its photos.
    @GetMapping
    public ResponseEntity<List<ImageResponse>> list(@PathVariable Long announcementId) {
        if (announcementService.getAnnouncementById(announcementId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponses(announcementId, imageService.list(announcementId)));
    }

    // Public. A photo never changes under the same id (replacing creates a new id), so it can be cached.
    @GetMapping("/{imageId}")
    public ResponseEntity<Resource> content(@PathVariable Long announcementId, @PathVariable Long imageId) {
        Optional<AnnouncementImage> image = imageService.find(announcementId, imageId);
        if (image.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = imageService.content(image.get());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.get().getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    /**
     * Replaces the whole gallery. Multipart fields:
     *  - layout: display order, e.g. "e:4,f:0,e:7" (e = keep existing photo id, f = index in files)
     *  - files:  new photos (optional)
     */
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImageResponse>> replace(
            @PathVariable Long announcementId,
            @RequestParam(value = "layout", required = false, defaultValue = "") String layout,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {

        Optional<Announcement> announcement = announcementService.getAnnouncementById(announcementId);
        if (announcement.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canManage(announcement.get(), authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<AnnouncementImage> saved = imageService.replace(announcementId, layout, files);
        return ResponseEntity.ok(toResponses(announcementId, saved));
    }

    // Same ownership rule as announcement updates: the listing agent or an admin.
    private static boolean canManage(Announcement announcement, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin || Objects.equals(announcement.getAgentId(), userId);
    }

    private static List<ImageResponse> toResponses(Long announcementId, List<AnnouncementImage> images) {
        return images.stream()
                .map(image -> new ImageResponse(
                        image.getId(),
                        "/api/announcements/" + announcementId + "/images/" + image.getId(),
                        image.getContentType(),
                        image.getPosition()))
                .toList();
    }
}
