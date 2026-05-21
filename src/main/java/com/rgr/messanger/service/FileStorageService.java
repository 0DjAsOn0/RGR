package com.rgr.messanger.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Сохраняет файл в подпапку folder.
     * @return публичный URL вида /uploads/{folder}/{uuid}.ext
     */
    String store(MultipartFile file, String folder);

    /**
     * Сохраняет аватар в /uploads/avatars.
     */
    String saveAvatar(MultipartFile file);

    /**
     * Удаляет файл по публичному URL.
     */
    void delete(String fileUrl);
}