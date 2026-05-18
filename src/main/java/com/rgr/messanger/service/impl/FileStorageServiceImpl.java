package com.rgr.messanger.service.impl;

import com.rgr.messanger.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size:52428800}") // 50MB
    private long maxFileSize;

    // Разрешённые типы
    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/ogg",
            "audio/mpeg", "audio/ogg", "audio/wav",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    @Override
    public String store(MultipartFile file, String folder) {
        if (file.isEmpty()) {
            throw new RuntimeException("Файл пустой");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("Файл слишком большой. Максимум 50MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_TYPES.contains(mimeType)) {
            throw new RuntimeException("Тип файла не разрешён: " + mimeType);
        }

        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(uploadDir, folder);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/" + uploadDir + "/" + folder + "/" + fileName;

        } catch (IOException e) {
            log.error("Ошибка сохранения файла: {}", e.getMessage());
            throw new RuntimeException("Ошибка сохранения файла");
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            Path path = Paths.get(fileUrl.substring(1)); // убираем ведущий /
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл: {}", fileUrl);
        }
    }
}
