package com.agenceimmobiliere.offers_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.agenceimmobiliere.offers_service.dto.AnnouncementResponse;

@Component
public class AnnouncementClient {

    private final RestClient restClient;
    private final String announcementsUrl;

    public AnnouncementClient(
            RestClient restClient,
            @Value("${announcements.service.url}") String announcementsUrl) {
        this.restClient = restClient;
        this.announcementsUrl = announcementsUrl;
    }

    public AnnouncementResponse getAnnouncement(Long announcementId) {

        return restClient.get()
                .uri(announcementsUrl + "/api/announcements/{id}", announcementId)
                .retrieve()
                .body(AnnouncementResponse.class);
    }
}