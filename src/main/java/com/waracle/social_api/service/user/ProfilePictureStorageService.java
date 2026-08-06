package com.waracle.social_api.service.user;

import com.waracle.social_api.exception.profile.InvalidProfilePictureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfilePictureStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    @Value("${app.upload.profile-pictures-dir:uploads/profile-pictures}")
    private String uploadDirectory;

    @Value("${app.upload.profile-pictures-url-prefix:/uploads/profile-pictures}")
    private String urlPrefix;

    @Value("${app.upload.profile-picture-max-size:5242880}")
    private long maxFileSize;

    private Path uploadPath;

    @PostConstruct
    void init() throws IOException {
        uploadPath = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
    }

    public String store(Long userId, MultipartFile file) {
        validate(file);

        String extension = resolveExtension(file);
        String filename = userId + "-" + UUID.randomUUID() + extension;
        Path target = uploadPath.resolve(filename).normalize();

        if (!target.startsWith(uploadPath)) {
            throw new InvalidProfilePictureException("Invalid file path.");
        }

        try {
            file.transferTo(target);
        } catch (IOException ex) {
            throw new InvalidProfilePictureException("Failed to store profile picture.");
        }

        return urlPrefix + "/" + filename;
    }

    public void deleteIfExists(String profilePictureUrl) {
        if (!StringUtils.hasText(profilePictureUrl) || !profilePictureUrl.startsWith(urlPrefix + "/")) {
            return;
        }

        String filename = profilePictureUrl.substring(urlPrefix.length() + 1);
        Path filePath = uploadPath.resolve(filename).normalize();

        if (!filePath.startsWith(uploadPath)) {
            return;
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Best-effort cleanup; DB update still succeeds.
        }
    }

    public Path getUploadPath() {
        return uploadPath;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidProfilePictureException("Profile picture file is required.");
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidProfilePictureException("Profile picture exceeds maximum allowed size.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidProfilePictureException("Unsupported profile picture type. Allowed: JPEG, PNG, WEBP, GIF.");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = StringUtils.getFilenameExtension(originalFilename);
            if (extension != null && !extension.isBlank()) {
                return "." + extension.toLowerCase();
            }
        }

        return switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }
}
