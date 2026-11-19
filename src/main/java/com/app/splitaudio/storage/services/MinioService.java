package com.app.splitaudio.storage.services;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.app.splitaudio.constants.Constants;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.expiration-hours}")
    private int expirationHours;

    public String uploadFile(Path localFilePath, String objectName) {

        log.info("*** Uploading Local file to MinIO: {}/{}", bucketName, objectName);

        try (InputStream is = Files.newInputStream(localFilePath)) {
            String contentType = Files.probeContentType(localFilePath);

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(is, Files.size(localFilePath), -1L)
                    .contentType(contentType != null ? contentType 
                        : Constants.App.APP_OCTET_STREAM)
                    .build()
            );

            log.info("*** Successfully uploaded: {}", objectName);
            return objectName;

        } catch(Exception e) {
            log.error("Error uploading Local file to MinIO", e);
            throw new RuntimeException("Failed to upload Local file to MinIO: " + e.getMessage(), 
                e);
        }
    }

    public void deleteFolder(String folderPrefix) {

        log.info("*** Triggering deletion for prefix: {}", folderPrefix);

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(folderPrefix)
                    .recursive(true)
                    .build()
            );

            List<String> objectsToDelete = new ArrayList<>();
            for (Result<Item> result : results) {
                objectsToDelete.add(result.get().objectName());
            }

            if (objectsToDelete.isEmpty()) {
                log.info("*** No files found to delete in prefix: {}", folderPrefix);
                return;
            }

            for (String objectName : objectsToDelete) {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
                );
            }

            log.info("*** Successfully deleted prefix (folder) and all its content: {}", 
                folderPrefix);

        } catch (Exception e) {
            log.error("*** Error trying to rollback/delete folder {}: {}", folderPrefix, 
                e.getMessage());
        }
    }

    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objectName)
                .expiry(expirationHours, TimeUnit.HOURS)
                .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error generating presigned URL for: " + objectName, e);
        }
    }
}
