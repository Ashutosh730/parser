package com.logAnalyzer.core.service.impl;

import com.logAnalyzer.core.model.StorageProperties;
import com.logAnalyzer.core.exception.StorageException;
import com.logAnalyzer.core.record.ObjectMetadata;
import com.logAnalyzer.core.record.StoreRequest;
import com.logAnalyzer.core.record.StoredObject;
import com.logAnalyzer.core.service.StorageService;
import com.logAnalyzer.core.util.StorageKeyUtil;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static com.logAnalyzer.core.util.StorageKeyUtil.resolve;

@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    private final Path baseDir;
    private final StorageProperties props;
    private final byte[] signingKey;

    public LocalStorageServiceImpl(StorageProperties props) {
        this.props = props;
        this.baseDir = Paths.get(System.getProperty("user.dir"), props.getLocalBaseDir())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new StorageException("Cannot create local storage directory: " + baseDir, e);
        }

        String secret = props.getLocalSigningSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("storage.local-signing-secret is not configured. " +
                    "Local presigned URLs will not be generated.");
            this.signingKey = null;
        } else {
            this.signingKey = Base64.getDecoder().decode(secret);
        }

        log.info("Local storage initialized at {}", baseDir);
    }

    // =====================================================
    // Persistence
    // =====================================================

    @Override
    public StoredObject store(StoreRequest req) {
        String key = StorageKeyUtil.buildKey(props.getKeyPrefix(), req.userId(), req.sessionId(), req.originalFileName()
        );
        Path target = resolve(baseDir, key);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = req.inputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Local store failed for key " + key, e);
        }

        long size = req.contentLength() >= 0 ? req.contentLength() : sizeOf(target);
        return new StoredObject(key, size, req.contentType(), providerName());
    }

    @Override
    public InputStream openStream(String key) {
        try {
            return Files.newInputStream(resolve(baseDir, key));
        } catch (IOException e) {
            throw new StorageException("Cannot open " + key, e);
        }
    }

    @Override
    public ObjectMetadata head(String key) {
        Path p = resolve(baseDir, key);
        try {
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
            return new ObjectMetadata(key, attrs.size(), null, attrs.lastModifiedTime().toInstant(), providerName()
            );
        } catch (IOException e) {
            throw new StorageException("head failed for " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(baseDir, key));
        } catch (IOException e) {
            throw new StorageException("delete failed for " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(baseDir, key));
    }

    // =====================================================
    // Access delegation
    // =====================================================

    @Override
    public String presignedDownloadUrl(String key, Duration ttl, String downloadFileName) {
        if (signingKey == null) {
            throw new UnsupportedOperationException(
                    "Local signing secret not configured; cannot produce signed URLs");
        }

        long exp = Instant.now().plus(ttl).getEpochSecond();
        String fileName = (downloadFileName == null || downloadFileName.isBlank())
                ? StorageKeyUtil.fileNameFromKey(key)
                : downloadFileName;

        String payload = key + "|" + exp + "|" + fileName;
        String sig = hmacBase64Url(payload);

        StringBuilder sb = new StringBuilder()
                .append(props.getLocalBaseUrl())
                .append("/api/files/download")
                .append("?key=").append(urlEncode(key))
                .append("&exp=").append(exp)
                .append("&name=").append(urlEncode(fileName))
                .append("&sig=").append(urlEncode(sig));

        return sb.toString();
    }

    @Override
    public String presignedUploadUrl(String key, Duration ttl, String contentType) {
        throw new UnsupportedOperationException(
                "Local storage does not support presigned uploads; use POST /api/logs/upload");
    }

    @Override
    public boolean supportsSigning() {
        return signingKey != null;
    }

    @Override
    public String providerName() {
        return "local";
    }

    // =====================================================
    // Signature verification (called by LocalFileController)
    // =====================================================

    /**
     * Constant-time check that a URL signature is valid and unexpired.
     * @throws StorageException if invalid or expired
     */
    public void verifySignature(String key, long exp, String fileName, String sig) {
        if (signingKey == null) {
            throw new StorageException("Local signing not enabled");
        }
        if (exp < Instant.now().getEpochSecond()) {
            throw new StorageException("Signed URL expired");
        }
        String payload = key + "|" + exp + "|" + fileName;
        String expected = hmacBase64Url(payload);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8))) {
            throw new StorageException("Invalid signature");
        }
    }

    // =====================================================
    // Helpers
    // =====================================================

    private long sizeOf(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            throw new StorageException("Cannot read size of " + p, e);
        }
    }

    private String hmacBase64Url(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new StorageException("HMAC computation failed", e);
        }
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}