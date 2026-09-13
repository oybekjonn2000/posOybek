package com.restaurantpos.products.service;

import com.restaurantpos.common.exception.PosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for securely uploading, storing, and deleting product images.
 */
@Service
@Slf4j
public class ProductImageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Uploads and stores a product image securely.
     *
     * @param file the uploaded MultipartFile
     * @return the relative web path to the stored image (e.g. "/uploads/products/xyz.webp")
     */
    public String uploadProductImage(MultipartFile file) {
        validateFile(file);

        try {
            Path targetDir = getProductsUploadPath();
            Files.createDirectories(targetDir);

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String safeBaseName = sanitizeFilename(originalFilename, extension);

            // Generate unique filename to avoid collisions and browser caching issues on replacement
            String uniqueFilename = UUID.randomUUID() + (safeBaseName.isEmpty() ? "" : "-" + safeBaseName) + "." + extension;

            Path targetPath = targetDir.resolve(uniqueFilename).normalize();

            // Guard against path traversal attacks
            if (!targetPath.startsWith(targetDir)) {
                throw PosException.badRequest("Xavfsizlik xatosi: Noto'g'ri fayl yo'li!");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Product image successfully saved: {}", targetPath);
            return "/uploads/products/" + uniqueFilename;
        } catch (IOException e) {
            log.error("Failed to save product image", e);
            throw PosException.internalError("Rasmni serverda saqlashda xatolik yuz berdi: " + e.getMessage());
        }
    }

    /**
     * Safely deletes an image file from the disk if it belongs to product uploads.
     *
     * @param imageUrl relative path to the image (e.g. "/uploads/products/xyz.webp")
     */
    public void deleteProductImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String prefix = "/uploads/products/";
            int index = imageUrl.indexOf(prefix);
            if (index == -1) {
                // If it doesn't match standard local upload path, do not attempt file deletion
                return;
            }

            String filename = imageUrl.substring(index + prefix.length());
            // Strip any query params if present
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf('?'));
            }

            Path targetDir = getProductsUploadPath();
            Path filePath = targetDir.resolve(filename).normalize();

            // Verify the file is strictly inside products upload directory
            if (!filePath.startsWith(targetDir)) {
                log.warn("Prevented deletion outside products directory: {}", filePath);
                return;
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted product image file: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("Could not delete product image file {}: {}", imageUrl, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw PosException.badRequest("Yuklangan rasm fayli bo'sh bo'lishi mumkin emas.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw PosException.badRequest("Rasm hajmi 5 MB dan oshmasligi kerak.");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw PosException.badRequest("Fayl nomi ko'rsatilmagan.");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw PosException.badRequest("Faqat JPG, JPEG, PNG yoki WEBP formatidagi rasm yuklash mumkin.");
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase(Locale.ROOT);
            if (!ALLOWED_MIME_TYPES.contains(contentType)) {
                throw PosException.badRequest("Faqat JPG, JPEG, PNG yoki WEBP formatidagi rasm yuklash mumkin.");
            }
        }

        validateMagicBytes(file);
    }

    private void validateMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw PosException.badRequest("Yuklangan fayl formati yaroqsiz yoki fayl juda kichik.");
            }

            boolean isJpeg = (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF);
            boolean isPng = (bytesRead >= 8 && header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A);
            boolean isWebp = (bytesRead >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P');

            if (!isJpeg && !isPng && !isWebp) {
                log.warn("Magic bytes security check failed for file: {}", file.getOriginalFilename());
                throw PosException.badRequest("Fayl xavfsizlik tekshiruvidan o'tmadi: Haqiqiy JPG, PNG yoki WEBP rasm fayli talab qilinadi.");
            }
        } catch (IOException e) {
            log.error("Error reading file header for magic bytes validation", e);
            throw PosException.badRequest("Faylni o'qishda xatolik yuz berdi.");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();
    }

    private String sanitizeFilename(String filename, String extension) {
        if (filename == null) return "";
        String base = filename;
        if (filename.contains(".")) {
            base = filename.substring(0, filename.lastIndexOf('.'));
        }
        // Remove special characters, keep alphanumeric, dashes, and underscores
        base = base.replaceAll("[^a-zA-Z0-9-_]", "-").replaceAll("-+", "-");
        if (base.length() > 30) {
            base = base.substring(0, 30);
        }
        return base.toLowerCase(Locale.ROOT);
    }

    private Path getProductsUploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve("products");
    }
}
