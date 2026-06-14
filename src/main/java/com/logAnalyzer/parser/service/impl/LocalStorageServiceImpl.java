package com.logAnalyzer.parser.service.impl;

import com.logAnalyzer.parser.config.FileStorageConfig;
import com.logAnalyzer.parser.service.StorageService;
import com.logAnalyzer.parser.util.LogFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LocalStorageServiceImpl implements StorageService {
    
    public final FileStorageConfig fileStorageConfig;

    @Override
    public Path upload(MultipartFile file) {
        String fileName = LogFileUtil.generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));

        Path uploadDir = Paths.get(System.getProperty("user.dir"), fileStorageConfig.getUploadDir());
        Path uploadPath = null;
        try {
            Files.createDirectories(uploadDir);
            uploadPath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return uploadPath;
    }
}
