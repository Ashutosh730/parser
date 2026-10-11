package com.logAnalyzer.core.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the storage layer.
 *
 * Bound from the "storage.*" prefix in application.yaml.
 *
 * Example:
 *   storage:
 *     type: s3
 *     bucket: log-analyzer-uploads
 *     region: ap-south-1
 *     key-prefix: logs
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** local | s3 | gcs | azure */
    private String type = "local";

    // ---------- Shared ----------

    /** Prefix applied to every key, e.g. "logs". */
    private String keyPrefix = "logs";

    /** Maximum size accepted for a single upload. */
    private long maxObjectSizeBytes = 100L * 1024 * 1024; // 100 MB

    /** Default TTL for presigned download URLs. */
    private int presignedUrlTtlMinutes = 15;

    // ---------- S3 / S3-compatible ----------

    private String bucket;
    private String region;

    /** Empty for real AWS. Set for MinIO / R2 / Backblaze / Wasabi. */
    private String endpoint;

    /** Optional; leave blank to use the AWS default provider chain. */
    private String accessKey;
    private String secretKey;

    /** true for MinIO and most S3-compatible providers. */
    private boolean pathStyleAccess = false;

    // ---------- Local filesystem ----------

    /** Base directory for local storage. Relative paths resolve under user.dir. */
    private String localBaseDir = "uploads";

    /** Public base URL of this backend — used to build self-hosted signed URLs. */
    private String localBaseUrl = "http://localhost:8081";

    /** HMAC secret for signing local download URLs. Must be Base64. */
    private String localSigningSecret;

}