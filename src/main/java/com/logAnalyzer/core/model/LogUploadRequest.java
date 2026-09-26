package com.logAnalyzer.core.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class LogUploadRequest {
    private MultipartFile file;
    private String fileType;
}
