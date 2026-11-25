package com.agenceimmobiliere.announcements_service.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.agenceimmobiliere.announcements_service.entity.AnnouncementImage;
import com.agenceimmobiliere.announcements_service.repository.AnnouncementImageRepository;

@Service
public class AnnouncementImageService {

    public static final int MAX_IMAGES = 5;
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final AnnouncementImageRepository repository;
    private final ImageStorage storage;

    public AnnouncementImageService(AnnouncementImageRepository repository, ImageStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public List<AnnouncementImage> list(Long announcementId) {
        return repository.findByAnnouncementIdOrderByPositionAsc(announcementId);
    }

    public Optional<AnnouncementImage> find(Long announcementId, Long imageId) {
        return repository.findByIdAndAnnouncementId(imageId, announcementId);
    }

    public org.springframework.core.io.Resource content(AnnouncementImage image) {
        return storage.load(image.getStoredName());
    }

    /**
     * Replaces the gallery of an announcement in one call.
     *
     * @param layout comma-separated, in display order (first = cover):
     *               "e:<imageId>" keeps an existing photo, "f:<index>" uses files[index].
     *               An empty layout removes every photo.
     */
    @Transactional
    public List<AnnouncementImage> replace(Long announcementId, String layout, List<MultipartFile> files) {
        List<MultipartFile> uploads = files == null ? List.of() : files;
        List<String> tokens = parseLayout(layout);

        if (tokens.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("A listing can have at most " + MAX_IMAGES + " photos.");
        }

        Map<Long, AnnouncementImage> current = new HashMap<>();
        for (AnnouncementImage image : list(announcementId)) {
            current.put(image.getId(), image);
        }

        // Validate everything before writing anything.
        Set<Long> kept = new HashSet<>();
        Set<Integer> usedFiles = new HashSet<>();
        Map<Integer, ValidatedFile> validated = new HashMap<>();
        for (String token : tokens) {
            if (token.startsWith("e:")) {
                Long id = parseNumber(token.substring(2));
                if (!current.containsKey(id) || !kept.add(id)) {
                    throw new IllegalArgumentException("Unknown or duplicated photo " + id + ".");
                }
            } else if (token.startsWith("f:")) {
                int index = parseNumber(token.substring(2)).intValue();
                if (index < 0 || index >= uploads.size() || !usedFiles.add(index)) {
                    throw new IllegalArgumentException("Invalid file reference " + index + ".");
                }
                validated.put(index, validate(uploads.get(index)));
            } else {
                throw new IllegalArgumentException("Invalid layout entry '" + token + "'.");
            }
        }
        if (usedFiles.size() != uploads.size()) {
            throw new IllegalArgumentException("Every uploaded file must appear in the layout.");
        }

        // Write new files; remove them again if anything below fails.
        List<String> written = new ArrayList<>();
        try {
            List<AnnouncementImage> result = new ArrayList<>();
            int position = 0;
            for (String token : tokens) {
                AnnouncementImage image;
                if (token.startsWith("e:")) {
                    image = current.get(parseNumber(token.substring(2)));
                } else {
                    ValidatedFile file = validated.get(parseNumber(token.substring(2)).intValue());
                    String name = storage.save(file.bytes(), file.extension());
                    written.add(name);
                    image = new AnnouncementImage();
                    image.setAnnouncementId(announcementId);
                    image.setStoredName(name);
                    image.setContentType(file.contentType());
                    image.setSizeBytes(file.bytes().length);
                }
                image.setPosition(position++);
                result.add(image);
            }

            List<AnnouncementImage> removed = current.values().stream()
                    .filter(image -> !kept.contains(image.getId()))
                    .toList();
            repository.deleteAll(removed);
            List<AnnouncementImage> saved = repository.saveAll(result);

            List<String> obsoleteFiles = removed.stream().map(AnnouncementImage::getStoredName).toList();
            afterCommit(() -> obsoleteFiles.forEach(storage::delete), () -> written.forEach(storage::delete));

            return saved.stream()
                    .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                    .toList();
        } catch (RuntimeException exception) {
            written.forEach(storage::delete);
            throw exception;
        }
    }

    @Transactional
    public void deleteAll(Long announcementId) {
        List<AnnouncementImage> images = list(announcementId);
        repository.deleteAll(images);
        List<String> files = images.stream().map(AnnouncementImage::getStoredName).toList();
        afterCommit(() -> files.forEach(storage::delete), () -> { });
    }

    private void afterCommit(Runnable onCommit, Runnable onRollback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            onCommit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    onCommit.run();
                } else {
                    onRollback.run();
                }
            }
        });
    }

    private record ValidatedFile(byte[] bytes, String contentType, String extension) {
    }

    // The type is decided from the file's first bytes, not from the name or the browser's claim.
    private ValidatedFile validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Each photo must be 5 MB or less.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        if (isJpeg(bytes)) {
            return new ValidatedFile(bytes, "image/jpeg", "jpg");
        }
        if (isPng(bytes)) {
            return new ValidatedFile(bytes, "image/png", "png");
        }
        if (isWebp(bytes)) {
            return new ValidatedFile(bytes, "image/webp", "webp");
        }
        throw new IllegalArgumentException("Only JPG, PNG and WebP photos are accepted.");
    }

    private static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (b.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((b[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWebp(byte[] b) {
        return b.length > 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private static List<String> parseLayout(String layout) {
        List<String> tokens = new ArrayList<>();
        if (layout == null || layout.isBlank()) {
            return tokens;
        }
        for (String part : layout.split(",")) {
            String token = part.trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static Long parseNumber(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid layout number '" + value + "'.");
        }
    }
}
