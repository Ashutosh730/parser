package com.logAnalyzer.core.service.impl;

import com.logAnalyzer.core.model.StorageProperties;
import com.logAnalyzer.core.exception.StorageException;
import com.logAnalyzer.core.record.ObjectMetadata;
import com.logAnalyzer.core.record.StoreRequest;
import com.logAnalyzer.core.record.StoredObject;
import com.logAnalyzer.core.service.StorageService;
import com.logAnalyzer.core.util.StorageKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3;
    private final StorageProperties props;
    private final S3Presigner presigner;


    @Override
    public StoredObject store(StoreRequest req) {
        String key = StorageKeyUtil.buildKey(props.getKeyPrefix(), req.userId(), req.sessionId(), req.originalFileName());

        PutObjectRequest.Builder b = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(req.contentType());

        if (req.contentLength() >= 0) {
            b.contentLength(req.contentLength());
        }
        if (req.metadata() != null && !req.metadata().isEmpty()) {
            b.metadata(req.metadata());
        }

        RequestBody body = req.contentLength() >= 0
                ? RequestBody.fromInputStream(req.inputStream(), req.contentLength())
                : RequestBody.fromBytes(readAll(req.inputStream()));   // avoid for large files

        try {
            s3.putObject(b.build(), body);
        } catch (SdkException e) {
            throw new StorageException("S3 put failed for key " + key, e);
        }

        return new StoredObject(key, req.contentLength(),
                req.contentType(), providerName());
    }

    @Override
    public InputStream openStream(String key) {
        try {
            return s3.getObject(GetObjectRequest.builder()
                    .bucket(props.getBucket()).key(key).build());
        } catch (SdkException e) {
            throw new StorageException("S3 get failed for key " + key, e);
        }
    }

    @Override
    public ObjectMetadata head(String key) {
        try {
            HeadObjectResponse h = s3.headObject(HeadObjectRequest.builder()
                    .bucket(props.getBucket()).key(key).build());
            return new ObjectMetadata(key, h.contentLength(), h.contentType(),
                    h.lastModified(), providerName());
        } catch (NoSuchKeyException e) {
            throw new StorageException("Not found: " + key, e);
        } catch (SdkException e) {
            throw new StorageException("S3 head failed for key " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket()).key(key).build());
        } catch (NoSuchKeyException e) {
            // idempotent
        } catch (SdkException e) {
            throw new StorageException("S3 delete failed for key " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(props.getBucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String providerName() {
        return "s3";
    }

    private byte[] readAll(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("read failed", e);
        }
    }

    @Override
    public String presignedDownloadUrl(String key, Duration ttl, String downloadFileName) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .responseContentDisposition("attachment; filename=\"" + downloadFileName + "\"")
                .build();

        return presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .getObjectRequest(get)
                        .build()
        ).url().toString();
    }

    @Override
    public String presignedUploadUrl(String key, Duration ttl, String contentType) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        return presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .putObjectRequest(put)
                        .build()
        ).url().toString();
    }

    @Override
    public boolean supportsSigning() {
        return true;
    }
}