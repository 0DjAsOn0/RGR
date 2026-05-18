package com.rgr.messanger.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String folder);
    void delete(String fileUrl);
}
