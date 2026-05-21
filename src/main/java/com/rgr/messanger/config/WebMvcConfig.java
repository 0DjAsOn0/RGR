package com.rgr.messanger.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadFolder = new File(uploadDir).getAbsoluteFile();

        if (!uploadFolder.exists() && !uploadFolder.mkdirs()) {
            throw new IllegalStateException(
                    "Не удалось создать директорию uploads: " + uploadFolder.getAbsolutePath()
            );
        }

        String location = uploadFolder.toURI().toString();

        log.info("Upload directory: {}", uploadFolder.getAbsolutePath());
        log.info("Upload resource location: {}", location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(0);
    }
}