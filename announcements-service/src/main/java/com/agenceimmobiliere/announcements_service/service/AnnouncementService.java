
package com.agenceimmobiliere.announcements_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.agenceimmobiliere.announcements_service.entity.Announcement;
import com.agenceimmobiliere.announcements_service.repository.AnnouncementRepository;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }
    

	public Optional<Announcement> getAnnouncementById(Long id) {
	    return announcementRepository.findById(id);
	}

	public Optional<Announcement> updateAnnouncement(
	        Long id, Announcement updatedAnnouncement) {

	    Optional<Announcement> existingAnnouncement =
	            announcementRepository.findById(id);

	    if (existingAnnouncement.isEmpty()) {
	        return Optional.empty();
	    }

	    Announcement announcement = existingAnnouncement.get();

	    announcement.setTitle(updatedAnnouncement.getTitle());
	    announcement.setDescription(updatedAnnouncement.getDescription());
	    announcement.setPrice(updatedAnnouncement.getPrice());
	    announcement.setPropertyType(updatedAnnouncement.getPropertyType());
	    announcement.setTransactionType(updatedAnnouncement.getTransactionType());
	    announcement.setCity(updatedAnnouncement.getCity());
	    announcement.setArea(updatedAnnouncement.getArea());

	    Announcement savedAnnouncement =
	            announcementRepository.save(announcement);

	    return Optional.of(savedAnnouncement);
	}
	
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    public Announcement createAnnouncement(Announcement announcement) {
        return announcementRepository.save(announcement);
    }
    
    public boolean deleteAnnouncement(Long id) {
    	
    	if(!announcementRepository.existsById(id)) {
    		return false;
    	}
    	
    	announcementRepository.deleteById(id);
    	
    	return true;
    }
}
