package com.rgr.messanger.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadFolder = new File(uploadDir).getAbsoluteFile();

        // Создаём папку если не существует
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
            log.info("📁 Создана папка: {}", uploadFolder.getAbsolutePath());
        }

        // toURI() автоматически правильно формирует путь
        // Windows: file:///D:/path/uploads/
        // Linux:   file:/home/user/uploads/
        String location = uploadFolder.toURI() + "/";

        log.info("📁 OS:       {}", System.getProperty("os.name"));
        log.info("📁 Путь:     {}", uploadFolder.getAbsolutePath());
        log.info("📁 Location: {}", location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(0);
    }
}