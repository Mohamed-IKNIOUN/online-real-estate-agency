package com.agence.favourites.controller;

import com.agence.favourites.entity.Favori;
import com.agence.favourites.service.FavoriService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FavoriControllerTest {

    private FavoriService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(FavoriService.class);
        mvc = MockMvcBuilders.standaloneSetup(new FavoriController(service)).build();
    }

    @Test
    void createsFavouriteAndReturnsIt() throws Exception {
        when(service.ajouter(1L, 10L)).thenReturn(new Favori(1L, 10L));

        mvc.perform(post("/api/favoris/{clientId}/{annonceId}", 1, 10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.annonceId").value(10));
    }

    @Test
    void listsFavouritesForClient() throws Exception {
        when(service.consulter(1L)).thenReturn(List.of(new Favori(1L, 10L)));

        mvc.perform(get("/api/favoris/client/{clientId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientId").value(1))
                .andExpect(jsonPath("$[0].annonceId").value(10));
    }

    @Test
    void deletesFavouriteAndReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/favoris/{clientId}/{annonceId}", 1, 10))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).supprimer(1L, 10L);
    }

    @Test
    void rejectsMalformedIdsBeforeCallingService() throws Exception {
        mvc.perform(post("/api/favoris/{clientId}/{annonceId}", 1, "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "Adding favourite for client {0} returns {1}")
    @CsvSource({"0, BAD_REQUEST", "1, CONFLICT", "1, NOT_FOUND", "1, SERVICE_UNAVAILABLE"})
    void returnsServiceErrorsWhenAddingFavourite(long clientId, HttpStatus error)
            throws Exception {
        when(service.ajouter(clientId, 10L)).thenThrow(new ResponseStatusException(error));

        mvc.perform(post("/api/favoris/{clientId}/{annonceId}", clientId, 10))
                .andExpect(status().is(error.value()));
    }

    @Test
    void returnsNotFoundWhenDeletingMissingFavourite() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(service).supprimer(1L, 10L);

        mvc.perform(delete("/api/favoris/{clientId}/{annonceId}", 1, 10))
                .andExpect(status().isNotFound());
    }
}
