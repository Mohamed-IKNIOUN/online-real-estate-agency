package com.agence.favourites.client;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnouncementClientTest {

    private HttpServer server;
    private AnnouncementClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        client = new AnnouncementClient(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/api/announcements");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void acceptsSuccessfulLookupUsingGetAndAnnouncementId() {
        startServerWithResponse(HttpStatus.OK);

        assertDoesNotThrow(() -> client.verifierAnnonce(10L));
    }

    @ParameterizedTest(name = "Upstream {0} maps to {1}")
    @CsvSource({"NOT_FOUND, NOT_FOUND", "INTERNAL_SERVER_ERROR, BAD_GATEWAY"})
    void mapsHttpErrors(HttpStatus upstreamStatus, HttpStatus expectedStatus) {
        startServerWithResponse(upstreamStatus);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> client.verifierAnnonce(10L));

        assertEquals(expectedStatus, error.getStatusCode());
    }

    @Test
    void returnsServiceUnavailableWhenUpstreamClosesConnection() {
        server.createContext("/api/announcements/10", exchange -> exchange.close());
        server.start();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> client.verifierAnnonce(10L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    private void startServerWithResponse(HttpStatus status) {
        server.createContext("/", exchange -> {
            try (exchange) {
                boolean expectedRequest = "GET".equals(exchange.getRequestMethod())
                        && "/api/announcements/10".equals(exchange.getRequestURI().getPath());
                int response = expectedRequest ? status.value() : HttpStatus.BAD_REQUEST.value();
                exchange.sendResponseHeaders(response, -1);
            }
        });
        server.start();
    }
}
