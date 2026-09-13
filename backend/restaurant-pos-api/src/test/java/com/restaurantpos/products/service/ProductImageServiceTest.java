package com.restaurantpos.products.service;

import com.restaurantpos.common.exception.PosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProductImageServiceTest {

    private ProductImageService productImageService;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        productImageService = new ProductImageService();
        ReflectionTestUtils.setField(productImageService, "uploadDir", tempUploadDir.toString());
    }

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'
    };
    private static final byte[] VALID_JPG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1
    };
    private static final byte[] VALID_WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8'
    };

    @Test
    void uploadProductImage_Success_Png() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pizza.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String imageUrl = productImageService.uploadProductImage(file);

        assertNotNull(imageUrl);
        assertTrue(imageUrl.startsWith("/uploads/products/"));
        assertTrue(imageUrl.endsWith(".png"));

        // Verify file exists on disk
        String filename = imageUrl.substring("/uploads/products/".length());
        Path savedFile = tempUploadDir.resolve("products").resolve(filename);
        assertTrue(Files.exists(savedFile));
    }

    @Test
    void uploadProductImage_Success_JpgAndWebp() {
        MockMultipartFile jpgFile = new MockMultipartFile(
                "file",
                "margarita.jpg",
                "image/jpeg",
                VALID_JPG_BYTES
        );
        String jpgUrl = productImageService.uploadProductImage(jpgFile);
        assertNotNull(jpgUrl);
        assertTrue(jpgUrl.endsWith(".jpg"));

        MockMultipartFile webpFile = new MockMultipartFile(
                "file",
                "burger.webp",
                "image/webp",
                VALID_WEBP_BYTES
        );
        String webpUrl = productImageService.uploadProductImage(webpFile);
        assertNotNull(webpUrl);
        assertTrue(webpUrl.endsWith(".webp"));
    }

    @Test
    void uploadProductImage_ThrowsOnMagicBytesMismatch() {
        // Spoofed file: extension is png, mime is image/png, but body is plaintext
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file",
                "malicious.png",
                "image/png",
                "echo 'Hacked!';".getBytes()
        );

        PosException ex = assertThrows(PosException.class, () ->
                productImageService.uploadProductImage(spoofedFile)
        );
        assertTrue(ex.getMessage().contains("xavfsizlik tekshiruvidan o'tmadi"));
    }

    @Test
    void uploadProductImage_ThrowsOnEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        PosException ex = assertThrows(PosException.class, () ->
                productImageService.uploadProductImage(emptyFile)
        );
        assertTrue(ex.getMessage().contains("bo'sh bo'lishi mumkin emas"));
    }

    @Test
    void uploadProductImage_ThrowsOnDisallowedFormat() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "malicious content".getBytes()
        );

        PosException ex = assertThrows(PosException.class, () ->
                productImageService.uploadProductImage(exeFile)
        );
        assertTrue(ex.getMessage().contains("Faqat JPG, JPEG, PNG yoki WEBP"));
    }

    @Test
    void uploadProductImage_ThrowsOnOversizedFile() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        System.arraycopy(VALID_PNG_BYTES, 0, largeBytes, 0, VALID_PNG_BYTES.length);
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "giant.png",
                "image/png",
                largeBytes
        );

        PosException ex = assertThrows(PosException.class, () ->
                productImageService.uploadProductImage(largeFile)
        );
        assertTrue(ex.getMessage().contains("5 MB"));
    }

    @Test
    void deleteProductImageFile_DeletesFileCorrectly() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pizza-delete.png",
                "image/png",
                VALID_PNG_BYTES
        );

        String imageUrl = productImageService.uploadProductImage(file);
        String filename = imageUrl.substring("/uploads/products/".length());
        Path savedFile = tempUploadDir.resolve("products").resolve(filename);
        assertTrue(Files.exists(savedFile));

        productImageService.deleteProductImageFile(imageUrl);
        assertFalse(Files.exists(savedFile));
    }
}
