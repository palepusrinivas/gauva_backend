package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.service.maps.MapsKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for location-related APIs including directions
 */
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

  private final MapsKeyService mapsKeyService;
  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Get directions between two points using Google Maps Directions API
   * 
   * GET /api/location/directions
   * 
   * Query Parameters:
   * - originLat (required) - Origin latitude
   * - originLng (required) - Origin longitude
   * - destinationLat (required) - Destination latitude
   * - destinationLng (required) - Destination longitude
   * - waypoints (optional) - Waypoints in format "lat1,lng1|lat2,lng2"
   * - language (optional) - Response language (default: en)
   * - googleMapsApiKey (optional) - If provided, uses this key; otherwise uses server key
   * 
   * Returns: Google Maps Directions API response
   */
  @GetMapping("/directions")
  public ResponseEntity<?> getDirections(
      @RequestParam("originLat") Double originLat,
      @RequestParam("originLng") Double originLng,
      @RequestParam("destinationLat") Double destinationLat,
      @RequestParam("destinationLng") Double destinationLng,
      @RequestParam(value = "waypoints", required = false) String waypoints,
      @RequestParam(value = "language", required = false, defaultValue = "en") String language,
      @RequestParam(value = "googleMapsApiKey", required = false) String clientApiKey) {
    
    // Validate required parameters
    if (originLat == null || originLng == null || destinationLat == null || destinationLng == null) {
      return new ResponseEntity<>(Map.of("error", "originLat, originLng, destinationLat, and destinationLng are required"), 
          HttpStatus.BAD_REQUEST);
    }

    // Get API key - prefer client-provided key, fallback to server key
    String apiKey;
    if (clientApiKey != null && !clientApiKey.isBlank()) {
      apiKey = clientApiKey;
      log.debug("Using client-provided Google Maps API key");
    } else {
      Optional<String> keyOpt = mapsKeyService.getServerKey();
      if (keyOpt.isEmpty()) {
        return new ResponseEntity<>(Map.of("error", "Google Maps API key not configured"), 
            HttpStatus.SERVICE_UNAVAILABLE);
      }
      apiKey = keyOpt.get();
      log.debug("Using server-configured Google Maps API key");
    }

    try {
      // Build Google Maps Directions API URL
      StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json");
      
      // Origin
      url.append("?origin=").append(originLat).append(",").append(originLng);
      
      // Destination
      url.append("&destination=").append(destinationLat).append(",").append(destinationLng);
      
      // Waypoints (optional)
      if (waypoints != null && !waypoints.isBlank()) {
        url.append("&waypoints=").append(URLEncoder.encode(waypoints, StandardCharsets.UTF_8));
      }
      
      // Language
      url.append("&language=").append(URLEncoder.encode(language, StandardCharsets.UTF_8));
      
      // API Key
      url.append("&key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));

      log.debug("Calling Google Maps Directions API: {}", url.toString().replace(apiKey, "***"));
      
      // Call Google Maps Directions API
      Map<?, ?> response = restTemplate.getForObject(URI.create(url.toString()), Map.class);
      
      if (response == null) {
        return new ResponseEntity<>(Map.of("error", "No response from Google Maps API"), 
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      log.error("Error calling Google Maps Directions API: {}", e.getMessage(), e);
      return new ResponseEntity<>(Map.of("error", "Failed to get directions: " + e.getMessage()), 
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}

