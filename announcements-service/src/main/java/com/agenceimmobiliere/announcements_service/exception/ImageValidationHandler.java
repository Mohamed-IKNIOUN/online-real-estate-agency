package com.agenceimmobiliere.announcements_service.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.agenceimmobiliere.announcements_service.controller.AnnouncementImageController;

/** Photo validation errors become 400s; scoped so existing endpoints keep their behaviour. */
@RestControllerAdvice(assignableTypes = AnnouncementImageController.class)
public class ImageValidationHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
