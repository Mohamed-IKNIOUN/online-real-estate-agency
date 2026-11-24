package com.agenceimmobiliere.offers_service.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.agenceimmobiliere.offers_service.dto.AnnouncementResponse;

@Component
public class AnnouncementClient {

    private final RestClient restClient;

    public AnnouncementClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public AnnouncementResponse getAnnouncement(Long announcementId) {

        return restClient.get()
                .uri("http://localhost:8081/api/announcements/{id}", announcementId)
                .retrieve()
                .body(AnnouncementResponse.class);
    }
}