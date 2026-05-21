package com.rgr.messanger.service.impl;

import com.rgr.messanger.exception.ResourceMappingException;
import com.rgr.messanger.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size:52428800}") // 50MB
    private long maxFileSize;

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024; // 5 MB
    private static final String AVATARS_FOLDER = "avatars";

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/ogg",
            "audio/mpeg", "audio/ogg", "audio/wav",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private static final Set<String> ALLOWED_FOLDERS = Set.of(
            "images", "videos", "audio", "files", "avatars"
    );

    private static final Set<String> AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // ========================
    // ОБЩЕЕ СОХРАНЕНИЕ
    // ========================
    @Override
    public String store(MultipartFile file, String folder) {
        validateFile(file, maxFileSize, ALLOWED_TYPES);
        validateFolder(folder);
        return saveToFolder(file, folder);
    }

    // ========================
    // СОХРАНИТЬ АВАТАР
    // ========================
    @Override
    public String saveAvatar(MultipartFile file) {
        validateFile(file, MAX_AVATAR_SIZE, AVATAR_TYPES);
        return saveToFolder(file, AVATARS_FOLDER);
    }

    // ========================
    // УДАЛИТЬ ФАЙЛ
    // ========================
    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String expectedPrefix = "/" + uploadDir + "/";
            if (!fileUrl.startsWith(expectedPrefix)) {
                log.warn("Попытка удалить файл вне upload dir: {}", fileUrl);
                return;
            }

            String relativePath = fileUrl.substring(expectedPrefix.length());
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetPath = baseDir.resolve(relativePath).normalize();

            if (!targetPath.startsWith(baseDir)) {
                log.warn("Попытка path traversal при удалении файла: {}", fileUrl);
                return;
            }

            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл {}: {}", fileUrl, e.getMessage());
        }
    }

    // ========================
    // PRIVATE
    // ========================

    private void validateFile(MultipartFile file, long maxSize, Set<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "Файл слишком большой. Максимум " + (maxSize / 1024 / 1024) + " MB"
            );
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !allowedTypes.contains(mimeType)) {
            throw new IllegalArgumentException("Тип файла не разрешён: " + mimeType);
        }
    }

    private void validateFolder(String folder) {
        if (folder == null || !ALLOWED_FOLDERS.contains(folder)) {
            throw new IllegalArgumentException("Недопустимая папка: " + folder);
        }
    }

    private String saveToFolder(MultipartFile file, String folder) {
        try {
            String extension = extractExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + extension;

            Path uploadPath = Paths.get(uploadDir, folder).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName).normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new IllegalArgumentException("Недопустимое имя файла");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/" + uploadDir + "/" + folder + "/" + fileName;

        } catch (IOException e) {
            log.error("Ошибка сохранения файла: {}", e.getMessage(), e);
            throw new ResourceMappingException("Ошибка сохранения файла", e);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        String ext = filename.substring(filename.lastIndexOf("."));
        return ext.length() <= 10 ? ext.toLowerCase() : "";
    }
}