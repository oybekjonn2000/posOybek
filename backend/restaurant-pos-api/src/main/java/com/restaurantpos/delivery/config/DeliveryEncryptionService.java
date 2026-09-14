package com.restaurantpos.delivery.config;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise AES-GCM 256-bit encryption service for delivery credentials
 * (API Key, Client Secret, Webhook Secret, etc.)
 */
@Service
public class DeliveryEncryptionService {

    // Default 256-bit key (in production can be overridden via ENV/vault)
    private static final byte[] STATIC_KEY = "RestaurantPosDeliverySecretKey99".getBytes(StandardCharsets.UTF_8);
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(STATIC_KEY, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Credential encryption failed", e);
        }
    }

    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.isBlank()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);
            if (decoded.length < GCM_IV_LENGTH) return null;

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherText = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(STATIC_KEY, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null; // Return null if corrupted or invalid rather than crashing
        }
    }

    public String mask(String secret) {
        if (secret == null || secret.isBlank()) return null;
        if (secret.length() <= 4) return "••••";
        return "••••••••" + secret.substring(secret.length() - 4);
    }
}
