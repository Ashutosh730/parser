package com.logAnalyzer.parser.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {
    Path upload(MultipartFile file);
}
