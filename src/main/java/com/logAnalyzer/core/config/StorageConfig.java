package com.logAnalyzer.core.config;

import com.logAnalyzer.core.model.StorageProperties;
import com.logAnalyzer.core.service.StorageService;
import com.logAnalyzer.core.service.impl.LocalStorageServiceImpl;
import com.logAnalyzer.core.service.impl.S3StorageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class StorageConfig {

    // =========================================================
    // Local filesystem
    // =========================================================

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
    public StorageService localStorageService(StorageProperties props) {
        log.info("Initializing LOCAL storage with base dir: {}", props.getLocalBaseDir());
        return new LocalStorageServiceImpl(props);
    }

    // =========================================================
    // AWS S3 (and S3-compatible: MinIO, R2, Backblaze, Wasabi)
    // =========================================================

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    public S3Client s3Client(StorageProperties props) {
        log.info("Initializing S3 client: bucket={}, region={}, endpoint={}, pathStyle={}",
                props.getBucket(), props.getRegion(), props.getEndpoint(), props.isPathStyleAccess());

        var builder = S3Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(resolveCredentials(props));

        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
        }

        if (props.isPathStyleAccess()) {
            builder.serviceConfiguration(
                    S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build()
            );
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    public S3Presigner s3Presigner(StorageProperties props) {
        log.info("Initializing S3 presigner: region={}, endpoint={}", props.getRegion(), props.getEndpoint());

        var builder = S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(resolveCredentials(props));

        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "s3")
    public StorageService s3StorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties props) {
        return new S3StorageServiceImpl(s3Client, props, s3Presigner);
    }

    /**
     * Prefer explicit static credentials if provided in properties,
     * otherwise fall back to the AWS default chain
     * (env vars → system props → profile → IAM role → container creds).
     */
    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
    resolveCredentials(StorageProperties props) {

        String accessKey = props.getAccessKey();
        String secretKey = props.getSecretKey();

        if (accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            log.info("Using static S3 credentials from configuration");
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            );
        }

        log.info("Using default AWS credentials provider chain (IAM role / env / profile)");
        return DefaultCredentialsProvider.create();
    }
}