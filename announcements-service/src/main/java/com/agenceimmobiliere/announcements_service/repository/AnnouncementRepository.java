
package com.agenceimmobiliere.announcements_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agenceimmobiliere.announcements_service.entity.Announcement;

public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

}
