package com.agenceimmobiliere.announcements_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agenceimmobiliere.announcements_service.entity.AnnouncementImage;

public interface AnnouncementImageRepository
        extends JpaRepository<AnnouncementImage, Long> {

    List<AnnouncementImage> findByAnnouncementIdOrderByPositionAsc(Long announcementId);

    Optional<AnnouncementImage> findByIdAndAnnouncementId(Long id, Long announcementId);
}
