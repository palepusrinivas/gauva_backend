package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.model.ApiKey;
import com.ridefast.ride_fast_backend.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing API keys stored in the database.
 * Keys can be configured by Admin/Super Admin from the admin panel.
 * Falls back to environment variables if database keys are not set.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    // Fallback values from environment
    @Value("${app.razorpay.key-id:}")
    private String envRazorpayKeyId;

    @Value("${app.razorpay.key-secret:}")
    private String envRazorpayKeySecret;

    @Value("${app.razorpay.webhook-secret:}")
    private String envRazorpayWebhookSecret;

    @Value("${app.google.maps-api-key:}")
    private String envGoogleMapsApiKey;

    @Value("${app.google.places-api-key:}")
    private String envGooglePlacesApiKey;

    @Value("${app.firebase.project-id:}")
    private String envFirebaseProjectId;

    @Value("${app.firebase.storage-bucket:}")
    private String envFirebaseStorageBucket;

    // API Key Names (constants)
    public static final String RAZORPAY_KEY_ID = "RAZORPAY_KEY_ID";
    public static final String RAZORPAY_KEY_SECRET = "RAZORPAY_KEY_SECRET";
    public static final String RAZORPAY_WEBHOOK_SECRET = "RAZORPAY_WEBHOOK_SECRET";
    public static final String GOOGLE_MAPS_API_KEY = "GOOGLE_MAPS_API_KEY";
    public static final String GOOGLE_PLACES_API_KEY = "GOOGLE_PLACES_API_KEY";
    public static final String FIREBASE_PROJECT_ID = "FIREBASE_PROJECT_ID";
    public static final String FIREBASE_STORAGE_BUCKET = "FIREBASE_STORAGE_BUCKET";
    public static final String FCM_SERVER_KEY = "FCM_SERVER_KEY";
    public static final String SMS_API_KEY = "SMS_API_KEY";
    public static final String LOCATIONIQ_API_KEY = "LOCATIONIQ_API_KEY";

    /**
     * Get API key value by name.
     * First checks database, then falls back to environment variable.
     */
    public String getKey(String keyName) {
        // Try database first
        Optional<ApiKey> dbKey = apiKeyRepository.findByName(keyName);
        if (dbKey.isPresent() && dbKey.get().getValue() != null && !dbKey.get().getValue().isBlank()) {
            log.debug("Using database API key for: {}", keyName);
            return dbKey.get().getValue();
        }

        // Fall back to environment variable
        String envValue = getEnvFallback(keyName);
        if (envValue != null && !envValue.isBlank()) {
            log.debug("Using environment API key for: {}", keyName);
            return envValue;
        }

        log.warn("No API key found for: {}", keyName);
        return null;
    }

    /**
     * Get API key with a default fallback
     */
    public String getKeyOrDefault(String keyName, String defaultValue) {
        String value = getKey(keyName);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    /**
     * Check if an API key is configured
     */
    public boolean isKeyConfigured(String keyName) {
        String value = getKey(keyName);
        return value != null && !value.isBlank();
    }

    /**
     * Set or update an API key
     */
    public ApiKey setKey(String keyName, String value, String description) {
        try {
            ApiKey apiKey = apiKeyRepository.findByName(keyName).orElse(null);
            
            if (apiKey == null) {
                // Create new
                apiKey = new ApiKey();
                apiKey.setName(keyName);
            }
            
            apiKey.setValue(value);
            if (description != null) {
                apiKey.setDescription(description);
            }
            
            return apiKeyRepository.save(apiKey);
        } catch (Exception e) {
            log.error("Error saving API key {}: {}", keyName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete an API key
     */
    public void deleteKey(String keyName) {
        apiKeyRepository.findByName(keyName).ifPresent(apiKeyRepository::delete);
    }

    /**
     * Clear cache - no-op since caching is disabled
     */
    public void clearCache() {
        log.info("API key cache clear requested (caching disabled)");
    }

    // Razorpay specific getters
    public String getRazorpayKeyId() {
        return getKey(RAZORPAY_KEY_ID);
    }

    public String getRazorpayKeySecret() {
        return getKey(RAZORPAY_KEY_SECRET);
    }

    public String getRazorpayWebhookSecret() {
        return getKey(RAZORPAY_WEBHOOK_SECRET);
    }

    // Google specific getters
    public String getGoogleMapsApiKey() {
        return getKey(GOOGLE_MAPS_API_KEY);
    }

    public String getGooglePlacesApiKey() {
        // Places API can use same key as Maps
        String placesKey = getKey(GOOGLE_PLACES_API_KEY);
        if (placesKey != null && !placesKey.isBlank()) {
            return placesKey;
        }
        return getGoogleMapsApiKey();
    }

    // Firebase specific getters
    public String getFirebaseProjectId() {
        return getKey(FIREBASE_PROJECT_ID);
    }

    public String getFirebaseStorageBucket() {
        return getKey(FIREBASE_STORAGE_BUCKET);
    }

    private String getEnvFallback(String keyName) {
        return switch (keyName) {
            case RAZORPAY_KEY_ID -> envRazorpayKeyId;
            case RAZORPAY_KEY_SECRET -> envRazorpayKeySecret;
            case RAZORPAY_WEBHOOK_SECRET -> envRazorpayWebhookSecret;
            case GOOGLE_MAPS_API_KEY -> envGoogleMapsApiKey;
            case GOOGLE_PLACES_API_KEY -> envGooglePlacesApiKey;
            case FIREBASE_PROJECT_ID -> envFirebaseProjectId;
            case FIREBASE_STORAGE_BUCKET -> envFirebaseStorageBucket;
            default -> null;
        };
    }
}

