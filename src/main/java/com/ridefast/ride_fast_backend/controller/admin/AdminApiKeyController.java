package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.ApiKey;
import com.ridefast.ride_fast_backend.repository.ApiKeyRepository;
import com.ridefast.ride_fast_backend.service.ApiKeyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API for managing API keys (Google, Razorpay, Firebase, etc.)
 * Access: ADMIN and SUPER_ADMIN roles
 */
@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
@Slf4j
public class AdminApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;

    /**
     * Get all API keys (values masked for security)
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllKeys() {
        List<ApiKey> keys = apiKeyRepository.findAll();
        
        // Mask values for security
        List<Map<String, Object>> maskedKeys = keys.stream().map(key -> {
            Map<String, Object> masked = new HashMap<>();
            masked.put("id", key.getId());
            masked.put("name", key.getName());
            masked.put("description", key.getDescription());
            masked.put("createdAt", key.getCreatedAt());
            masked.put("isConfigured", key.getValue() != null && !key.getValue().isBlank());
            masked.put("maskedValue", maskValue(key.getValue()));
            return masked;
        }).toList();
        
        return ResponseEntity.ok(maskedKeys);
    }

    /**
     * Get API key configuration status (which keys are set)
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getKeyStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Razorpay status
        Map<String, Boolean> razorpay = new HashMap<>();
        razorpay.put("keyId", apiKeyService.isKeyConfigured(ApiKeyService.RAZORPAY_KEY_ID));
        razorpay.put("keySecret", apiKeyService.isKeyConfigured(ApiKeyService.RAZORPAY_KEY_SECRET));
        razorpay.put("webhookSecret", apiKeyService.isKeyConfigured(ApiKeyService.RAZORPAY_WEBHOOK_SECRET));
        status.put("razorpay", razorpay);
        
        // Google status
        Map<String, Boolean> google = new HashMap<>();
        google.put("mapsApiKey", apiKeyService.isKeyConfigured(ApiKeyService.GOOGLE_MAPS_API_KEY));
        google.put("placesApiKey", apiKeyService.isKeyConfigured(ApiKeyService.GOOGLE_PLACES_API_KEY));
        status.put("google", google);
        
        // Firebase status
        Map<String, Boolean> firebase = new HashMap<>();
        firebase.put("projectId", apiKeyService.isKeyConfigured(ApiKeyService.FIREBASE_PROJECT_ID));
        firebase.put("storageBucket", apiKeyService.isKeyConfigured(ApiKeyService.FIREBASE_STORAGE_BUCKET));
        firebase.put("fcmServerKey", apiKeyService.isKeyConfigured(ApiKeyService.FCM_SERVER_KEY));
        status.put("firebase", firebase);
        
        // Other
        Map<String, Boolean> other = new HashMap<>();
        other.put("smsApiKey", apiKeyService.isKeyConfigured(ApiKeyService.SMS_API_KEY));
        other.put("locationIqApiKey", apiKeyService.isKeyConfigured(ApiKeyService.LOCATIONIQ_API_KEY));
        status.put("other", other);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Get a specific API key by name (value masked)
     */
    @GetMapping("/{keyName}")
    public ResponseEntity<?> getKey(@PathVariable String keyName) {
        return apiKeyRepository.findByName(keyName.toUpperCase())
                .map(key -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", key.getId());
                    response.put("name", key.getName());
                    response.put("description", key.getDescription());
                    response.put("createdAt", key.getCreatedAt());
                    response.put("isConfigured", key.getValue() != null && !key.getValue().isBlank());
                    response.put("maskedValue", maskValue(key.getValue()));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Set/Update an API key
     */
    @PutMapping("/{keyName}")
    public ResponseEntity<?> setKey(@PathVariable String keyName, @RequestBody SetKeyRequest request) {
        try {
            String normalizedName = keyName.toUpperCase().trim();
            
            if (request.getValue() == null || request.getValue().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Value is required"));
            }

            ApiKey savedKey = apiKeyService.setKey(normalizedName, request.getValue(), request.getDescription());
            log.info("API key updated: {}", normalizedName);

            return ResponseEntity.ok(Map.of(
                    "id", savedKey.getId(),
                    "name", savedKey.getName(),
                    "description", savedKey.getDescription(),
                    "message", "API key updated successfully"
            ));
        } catch (Exception e) {
            log.error("Error setting API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set API key: " + e.getMessage()));
        }
    }

    /**
     * Bulk update multiple API keys
     */
    @PutMapping("/bulk")
    public ResponseEntity<?> setBulkKeys(@RequestBody Map<String, String> keys) {
        try {
            int updated = 0;
            for (Map.Entry<String, String> entry : keys.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isBlank()) {
                    apiKeyService.setKey(entry.getKey().toUpperCase(), entry.getValue(), null);
                    updated++;
                }
            }
            
            log.info("Bulk API key update: {} keys updated", updated);
            return ResponseEntity.ok(Map.of(
                    "updated", updated,
                    "message", updated + " API keys updated successfully"
            ));
        } catch (Exception e) {
            log.error("Error in bulk key update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update keys: " + e.getMessage()));
        }
    }

    /**
     * Delete an API key
     */
    @DeleteMapping("/{keyName}")
    public ResponseEntity<?> deleteKey(@PathVariable String keyName) {
        try {
            apiKeyService.deleteKey(keyName.toUpperCase());
            log.info("API key deleted: {}", keyName.toUpperCase());
            return ResponseEntity.ok(Map.of("message", "API key deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete API key"));
        }
    }

    /**
     * Clear API key cache
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        apiKeyService.clearCache();
        return ResponseEntity.ok(Map.of("message", "API key cache cleared"));
    }

    /**
     * Test Razorpay configuration
     */
    @GetMapping("/test/razorpay")
    public ResponseEntity<?> testRazorpay() {
        try {
            String keyId = apiKeyService.getRazorpayKeyId();
            String keySecret = apiKeyService.getRazorpayKeySecret();
            
            if (keyId == null || keySecret == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "not_configured",
                        "message", "Razorpay keys are not configured"
                ));
            }

            // Try to create a Razorpay client
            com.razorpay.RazorpayClient client = new com.razorpay.RazorpayClient(keyId, keySecret);
            // Just creating the client validates the keys
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Razorpay configuration is valid",
                    "keyIdPrefix", keyId.substring(0, Math.min(10, keyId.length())) + "..."
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Razorpay configuration error: " + e.getMessage()
            ));
        }
    }

    /**
     * Test Google Maps configuration
     */
    @GetMapping("/test/google-maps")
    public ResponseEntity<?> testGoogleMaps() {
        try {
            String apiKey = apiKeyService.getGoogleMapsApiKey();
            
            if (apiKey == null || apiKey.isBlank()) {
                return ResponseEntity.ok(Map.of(
                        "status", "not_configured",
                        "message", "Google Maps API key is not configured"
                ));
            }

            // Simple test - try to call geocoding API
            String testUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=test&key=" + apiKey;
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            Map<?, ?> response = restTemplate.getForObject(testUrl, Map.class);
            
            String status = response != null ? response.get("status").toString() : "UNKNOWN";
            
            if ("REQUEST_DENIED".equals(status)) {
                return ResponseEntity.ok(Map.of(
                        "status", "error",
                        "message", "Google Maps API key is invalid or API not enabled"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Google Maps configuration is valid",
                    "apiKeyPrefix", apiKey.substring(0, Math.min(10, apiKey.length())) + "..."
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Google Maps configuration error: " + e.getMessage()
            ));
        }
    }

    /**
     * Initialize default API keys (creates entries without values)
     */
    @PostMapping("/init-defaults")
    public ResponseEntity<?> initDefaults() {
        try {
            List<Map<String, String>> defaults = List.of(
                    Map.of("name", ApiKeyService.RAZORPAY_KEY_ID, "description", "Razorpay API Key ID for payments"),
                    Map.of("name", ApiKeyService.RAZORPAY_KEY_SECRET, "description", "Razorpay API Key Secret"),
                    Map.of("name", ApiKeyService.RAZORPAY_WEBHOOK_SECRET, "description", "Razorpay Webhook Secret for verification"),
                    Map.of("name", ApiKeyService.GOOGLE_MAPS_API_KEY, "description", "Google Maps API Key for location services"),
                    Map.of("name", ApiKeyService.GOOGLE_PLACES_API_KEY, "description", "Google Places API Key for autocomplete"),
                    Map.of("name", ApiKeyService.FIREBASE_PROJECT_ID, "description", "Firebase Project ID"),
                    Map.of("name", ApiKeyService.FIREBASE_STORAGE_BUCKET, "description", "Firebase Storage Bucket URL"),
                    Map.of("name", ApiKeyService.FCM_SERVER_KEY, "description", "Firebase Cloud Messaging Server Key"),
                    Map.of("name", ApiKeyService.SMS_API_KEY, "description", "SMS Gateway API Key"),
                    Map.of("name", ApiKeyService.LOCATIONIQ_API_KEY, "description", "LocationIQ API Key for geocoding")
            );

            int created = 0;
            for (Map<String, String> def : defaults) {
                if (!apiKeyRepository.existsByName(def.get("name"))) {
                    ApiKey key = ApiKey.builder()
                            .name(def.get("name"))
                            .value("")
                            .description(def.get("description"))
                            .build();
                    apiKeyRepository.save(key);
                    created++;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "created", created,
                    "message", created + " default API key entries created"
            ));
        } catch (Exception e) {
            log.error("Error initializing defaults", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initialize defaults"));
        }
    }

    private String maskValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 8) {
            return "********";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    @Data
    public static class SetKeyRequest {
        private String value;
        private String description;
    }
}

