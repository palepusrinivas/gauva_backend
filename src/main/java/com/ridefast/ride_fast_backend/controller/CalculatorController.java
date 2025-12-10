package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.service.CalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for distance and calculation utilities
 */
@RestController
@RequestMapping("/api/calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculatorService calculatorService;

    /**
     * Calculate distance between two coordinates
     * 
     * GET /api/calculator/distance
     * 
     * Query parameters:
     * - sourceLat: Source latitude
     * - sourceLng: Source longitude
     * - destLat: Destination latitude
     * - destLng: Destination longitude
     * 
     * Returns distance in kilometers
     */
    @GetMapping("/distance")
    public ResponseEntity<Map<String, Object>> calculateDistance(
            @RequestParam("sourceLat") double sourceLat,
            @RequestParam("sourceLng") double sourceLng,
            @RequestParam("destLat") double destLat,
            @RequestParam("destLng") double destLng
    ) {
        double distanceKm = calculatorService.calculateDistance(sourceLat, sourceLng, destLat, destLng);
        
        return ResponseEntity.ok(Map.of(
            "distanceKm", Math.round(distanceKm * 100.0) / 100.0, // Round to 2 decimal places
            "distanceMeters", Math.round(distanceKm * 1000.0),
            "source", Map.of("latitude", sourceLat, "longitude", sourceLng),
            "destination", Map.of("latitude", destLat, "longitude", destLng)
        ));
    }

    /**
     * Calculate distance between two coordinates (POST version)
     * 
     * POST /api/calculator/distance
     * 
     * Request body:
     * {
     *   "sourceLat": 12.9716,
     *   "sourceLng": 77.5946,
     *   "destLat": 13.0827,
     *   "destLng": 80.2707
     * }
     */
    @PostMapping("/distance")
    public ResponseEntity<Map<String, Object>> calculateDistancePost(
            @RequestBody Map<String, Double> request
    ) {
        Double sourceLat = request.get("sourceLat");
        Double sourceLng = request.get("sourceLng");
        Double destLat = request.get("destLat");
        Double destLng = request.get("destLng");

        if (sourceLat == null || sourceLng == null || destLat == null || destLng == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing required parameters: sourceLat, sourceLng, destLat, destLng"
            ));
        }

        double distanceKm = calculatorService.calculateDistance(sourceLat, sourceLng, destLat, destLng);
        
        return ResponseEntity.ok(Map.of(
            "distanceKm", Math.round(distanceKm * 100.0) / 100.0,
            "distanceMeters", Math.round(distanceKm * 1000.0),
            "source", Map.of("latitude", sourceLat, "longitude", sourceLng),
            "destination", Map.of("latitude", destLat, "longitude", destLng)
        ));
    }
}
