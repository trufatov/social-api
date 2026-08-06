package com.waracle.social_api.service.user;

import com.waracle.social_api.exception.profile.InvalidProfilePictureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfilePictureStorageServiceTest {

    @TempDir
    Path tempDir;

    private ProfilePictureStorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        storageService = new ProfilePictureStorageService();
        ReflectionTestUtils.setField(storageService, "uploadDirectory", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "urlPrefix", "/uploads/profile-pictures");
        ReflectionTestUtils.setField(storageService, "maxFileSize", 5242880L);
        storageService.init();
    }

    @Test
    void validPngFile_store_returnsPublicUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        String url = storageService.store(1L, file);

        assertThat(url).startsWith("/uploads/profile-pictures/1-");
        assertThat(url).endsWith(".png");
    }

    @Test
    void emptyFile_store_throwsInvalidProfilePictureException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storageService.store(1L, file))
                .isInstanceOf(InvalidProfilePictureException.class);
    }

    @Test
    void unsupportedFileType_store_throwsInvalidProfilePictureException() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> storageService.store(1L, file))
                .isInstanceOf(InvalidProfilePictureException.class);
    }

    @Test
    void storedFileExists_deleteIfExists_removesFileFromDisk() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[]{9, 9, 9});
        String url = storageService.store(1L, file);

        storageService.deleteIfExists(url);

        assertThat(tempDir.toFile().listFiles()).isEmpty();
    }

    @Test
    void oversizedFile_store_throwsInvalidProfilePictureException() {
        ReflectionTestUtils.setField(storageService, "maxFileSize", 2L);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> storageService.store(1L, file))
                .isInstanceOf(InvalidProfilePictureException.class);
    }

    @Test
    void externalUrl_deleteIfExists_doesNothing() {
        storageService.deleteIfExists("https://cdn.example.com/pic.png");
        // no exception, no files created
    }
}
