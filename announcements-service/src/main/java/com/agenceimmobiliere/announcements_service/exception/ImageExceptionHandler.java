package com.agenceimmobiliere.announcements_service.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/** Turns photo validation failures into readable 400/413 responses instead of 500s. */
@RestControllerAdvice
public class ImageExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("message", "Each photo must be 5 MB or less."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> badMultipart(MultipartException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid photo upload."));
    }
}
