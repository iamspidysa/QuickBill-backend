package com.saurabh.quickbill.service.impl;

import com.saurabh.quickbill.exception.InvalidFileException;
import com.saurabh.quickbill.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${aws.bucket.name}")
    private String bucketName;

    private final S3Client s3Client;

    // ── Whitelist: MIME type → safe file extension ────────────────────────
    // Extension is derived from this map, never from getOriginalFilename().
    // getOriginalFilename() is user-controlled and can contain path traversal
    // sequences like "../../etc/passwd" or null entirely.
    private static final Map<String, String> ALLOWED_MIME_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png",  "png",
            "image/webp", "webp",
            "image/gif",  "gif"
    );

    // 5 MB in bytes
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public String uploadFile(MultipartFile file) {

        // ── 1. Reject empty files ─────────────────────────────────────────
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file provided.");
        }

        // ── 2. Enforce size limit ─────────────────────────────────────────
        // Checked before reading bytes to avoid buffering huge payloads.
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException(
                    "File too large. Maximum allowed size is 5 MB.");
        }

        // ── 3. Validate MIME type against the whitelist ───────────────────
        // getContentType() comes from the HTTP Content-Type header set by the
        // client — it can be spoofed. But combined with the whitelist it stops
        // the most common attacks (executables, SVGs with scripts, HTML files).
        // For stronger guarantees, add Apache Tika to sniff the actual bytes.
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.containsKey(mimeType)) {
            throw new InvalidFileException(
                    "Invalid file type. Only JPEG, PNG, WEBP, and GIF images are allowed.");
        }

        // ── 4. Derive extension from MIME type, never from filename ───────
        // getOriginalFilename() is attacker-controlled. Deriving the extension
        // from the whitelisted MIME map means the stored key is always safe.
        String extension = ALLOWED_MIME_TYPES.get(mimeType);
        String key = UUID.randomUUID() + "." + extension;

        // ── 5. Upload to S3 ───────────────────────────────────────────────
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl("public-read")
                    .contentType(mimeType)
                    .build();

            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            if (response.sdkHttpResponse().isSuccessful()) {
                return "https://" + bucketName + ".s3.amazonaws.com/" + key;
            }

            throw new RuntimeException("Upload failed — S3 returned a non-success response.");

        } catch (IOException e) {
            throw new RuntimeException("Could not read file bytes for upload.", e);
        }
    }

    @Override
    public boolean deleteFile(String imgUrl) {
        String filename = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(filename)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
        return true;
    }
}