package com.restaurantpos.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Automated Security & Vulnerability Remediation Integration Test Suite.
 * Validates OWASP API Security Top 10 defenses:
 * - BOLA / Role Escalation
 * - Waiter Isolation
 * - Broken Authentication / Unauthenticated access
 * - Input & Quantity Validation
 * - Status Manipulation Guards
 * - File Upload Magic Bytes Validation
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRemediationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'
    };

    // =========================================================================
    // 1. UNHEALTHY / UNAUTHENTICATED ACCESS (Broken Authentication)
    // =========================================================================
    @Test
    @DisplayName("SEC-AUTH-01: Unauthenticated request to /api/users must return 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-AUTH-02: Unauthenticated request to /api/orders must return 401 Unauthorized")
    void unauthenticatedRequest_Orders_Returns401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. PRIVILEGE ESCALATION / RBAC (Broken Function Level Authorization)
    // =========================================================================
    @Test
    @WithMockUser(username = "waiter", authorities = {"ROLE_WAITER", "CREATE_ORDER", "EDIT_ORDER"})
    @DisplayName("SEC-RBAC-01: Waiter role must be rejected from Admin Users management (403 Forbidden)")
    void waiterCannotAccessUsersManagement_Returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "waiter", authorities = {"ROLE_WAITER", "CREATE_ORDER", "EDIT_ORDER"})
    @DisplayName("SEC-RBAC-02: Waiter role must be rejected from System Diagnostic Info (403 Forbidden)")
    void waiterCannotAccessSystemInfo_Returns403() throws Exception {
        mockMvc.perform(get("/api/settings/system/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "waiter", authorities = {"ROLE_WAITER", "CREATE_ORDER", "EDIT_ORDER"})
    @DisplayName("SEC-RBAC-03: Waiter role must be rejected from Warehouse Stock Adjustment (403 Forbidden)")
    void waiterCannotAccessStockAdjustment_Returns403() throws Exception {
        UUID fakeItemId = UUID.randomUUID();
        mockMvc.perform(patch("/api/inventory/" + fakeItemId + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 10, \"notes\": \"Malicious adjustment\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "kitchen", authorities = {"ROLE_KITCHEN", "KITCHEN_VIEW", "KITCHEN_UPDATE"})
    @DisplayName("SEC-RBAC-04: Kitchen staff must be rejected from Employee management (403 Forbidden)")
    void kitchenCannotAccessUsersManagement_Returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "kitchen", authorities = {"ROLE_KITCHEN", "KITCHEN_VIEW", "KITCHEN_UPDATE"})
    @DisplayName("SEC-RBAC-05: Kitchen staff must be rejected from modifying inventory (403 Forbidden)")
    void kitchenCannotAccessInventoryAdjustment_Returns403() throws Exception {
        UUID fakeItemId = UUID.randomUUID();
        mockMvc.perform(patch("/api/inventory/" + fakeItemId + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 50, \"notes\": \"Kitchen adjust\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cashier", authorities = {"ROLE_CASHIER", "PROCESS_PAYMENT", "VIEW_DASHBOARD"})
    @DisplayName("SEC-RBAC-06: Cashier must be rejected from Warehouse adjustments (403 Forbidden)")
    void cashierCannotAccessInventoryAdjustment_Returns403() throws Exception {
        UUID fakeItemId = UUID.randomUUID();
        mockMvc.perform(patch("/api/inventory/" + fakeItemId + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 100}"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. INPUT VALIDATION & QUANTITY MANIPULATION (API6:2023)
    // =========================================================================
    @Test
    @WithMockUser(username = "waiter", authorities = {"ROLE_WAITER", "CREATE_ORDER", "EDIT_ORDER"})
    @DisplayName("SEC-INPUT-01: Order item with negative quantity must be rejected (400 Bad Request)")
    void negativeQuantityOrderItem_Returns400() throws Exception {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                    "items": [
                        {
                            "productId": "e0000000-0000-0000-0000-000000000001",
                            "quantity": -5
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/orders/" + orderId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "waiter", authorities = {"ROLE_WAITER", "CREATE_ORDER", "EDIT_ORDER"})
    @DisplayName("SEC-INPUT-02: Order item with zero quantity must be rejected (400 Bad Request)")
    void zeroQuantityOrderItem_Returns400() throws Exception {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                    "items": [
                        {
                            "productId": "e0000000-0000-0000-0000-000000000001",
                            "quantity": 0
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/orders/" + orderId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 4. STATUS & DISCOUNT MANIPULATION GUARDS (API3 & Business Logic Abuse)
    // =========================================================================
    @Test
    @WithMockUser(username = "waiter", authorities = {"ROLE_WAITER", "CREATE_ORDER", "EDIT_ORDER"})
    @DisplayName("SEC-STATUS-01: Updating order status to PAID via status endpoint must be rejected (400 Bad Request)")
    void manualSettingPaidStatus_Returns400() throws Exception {
        UUID orderId = UUID.randomUUID();
        String payload = "{\"status\": \"PAID\"}";

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "cashier", authorities = {"ROLE_CASHIER", "APPLY_DISCOUNT"})
    @DisplayName("SEC-DISC-01: Negative discount percent must be rejected (400 Bad Request)")
    void negativeDiscountPercent_Returns400() throws Exception {
        UUID orderId = UUID.randomUUID();
        String payload = "{\"percent\": -15.0}";

        mockMvc.perform(put("/api/orders/" + orderId + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 5. FILE UPLOAD MAGIC BYTES SECURITY (File Upload & Path Traversal)
    // =========================================================================
    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "MANAGE_PRODUCTS"})
    @DisplayName("SEC-UPLOAD-01: Spoofed image file (text file renamed to .png) must fail magic bytes verification (400)")
    void spoofedImageFile_RejectedByMagicBytes() throws Exception {
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "shell.png",
                "image/png",
                "#!/bin/bash\necho Malicious script;".getBytes()
        );

        mockMvc.perform(multipart("/api/products/upload-image").file(fakeFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "MANAGE_PRODUCTS"})
    @DisplayName("SEC-UPLOAD-02: Valid PNG with authentic magic bytes must succeed (200 OK)")
    void validPngImage_Accepted() throws Exception {
        MockMultipartFile realFile = new MockMultipartFile(
                "file",
                "real_photo.png",
                "image/png",
                VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/products/upload-image").file(realFile))
                .andExpect(status().isOk());
    }
}
