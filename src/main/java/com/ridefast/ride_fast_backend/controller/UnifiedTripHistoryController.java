package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.UnifiedTripHistoryDto;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.service.TripHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified trip history controller that returns both regular rides and intercity bookings
 * Works for both users and drivers
 */
@RestController
@RequestMapping("/api/v1/trip-history")
@RequiredArgsConstructor
public class UnifiedTripHistoryController {

    private final TripHistoryService tripHistoryService;

    /**
     * Get unified trip history (regular rides + intercity bookings) for user or driver
     * 
     * GET /api/v1/trip-history?page=0&size=10
     * 
     * @param jwtToken Authorization header with Bearer token
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Page of unified trip history DTOs
     */
    @GetMapping
    public ResponseEntity<Page<UnifiedTripHistoryDto>> getUnifiedTripHistory(
            @RequestHeader("Authorization") String jwtToken,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) throws UserException, ResourceNotFoundException {
        Page<UnifiedTripHistoryDto> history = tripHistoryService.getUnifiedTripHistory(jwtToken, page, size);
        return ResponseEntity.ok(history);
    }
}
