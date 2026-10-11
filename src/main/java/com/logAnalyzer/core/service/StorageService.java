package com.logAnalyzer.core.service;

import com.logAnalyzer.core.record.ObjectMetadata;
import com.logAnalyzer.core.record.StoreRequest;
import com.logAnalyzer.core.record.StoredObject;

import java.io.InputStream;
import java.time.Duration;

/**
 * Storage abstraction combining persistence and access delegation.
 *
 * <p><b>Persistence</b> ({@code store/openStream/head/delete/exists}) behaves
 * uniformly across all providers.
 *
 * <p><b>Access delegation</b> ({@code presignedDownloadUrl} / {@code presignedUploadUrl})
 * differs by provider:
 * <ul>
 *   <li>S3 / GCS / Azure — URL points directly to the cloud provider.</li>
 *   <li>Local — URL points to a signed endpoint on this backend.</li>
 * </ul>
 *
 * <p>If a provider cannot fulfil an operation, it must throw
 * {@link UnsupportedOperationException}. Callers should check
 * {@link #supportsSigning()} first.
 */
public interface StorageService {

    // ---------- Persistence ----------

    StoredObject store(StoreRequest request);

    InputStream openStream(String key);

    ObjectMetadata head(String key);

    void delete(String key);

    boolean exists(String key);

    // ---------- Access delegation ----------

    /**
     * @param ttl lifetime of the URL
     * @param downloadFileName suggested filename; may be null
     * @throws UnsupportedOperationException if the provider cannot sign
     */
    String presignedDownloadUrl(String key, Duration ttl, String downloadFileName);

    /**
     * @throws UnsupportedOperationException if the provider does not
     *         support direct uploads (e.g. local filesystem)
     */
    String presignedUploadUrl(String key, Duration ttl, String contentType);

    boolean supportsSigning();

    String providerName();
}