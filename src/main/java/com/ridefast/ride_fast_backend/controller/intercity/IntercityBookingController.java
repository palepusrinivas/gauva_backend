package com.ridefast.ride_fast_backend.controller.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.*;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.service.intercity.IntercityBookingService;
import com.ridefast.ride_fast_backend.service.intercity.IntercityPricingService;
import com.ridefast.ride_fast_backend.service.intercity.IntercityTripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Customer-facing API for intercity bookings
 */
@RestController
@RequestMapping("/api/customer/intercity")
@RequiredArgsConstructor
@Slf4j
public class IntercityBookingController {
    
    private final IntercityBookingService bookingService;
    private final IntercityTripService tripService;
    private final IntercityPricingService pricingService;
    
    /**
     * Search for available trips and vehicle options
     * 
     * POST /api/customer/intercity/search
     */
    @PostMapping("/search")
    public ResponseEntity<IntercityTripSearchResponse> searchTrips(
            @Valid @RequestBody IntercityTripSearchRequest request
    ) {
        IntercityTripSearchResponse response = bookingService.searchTrips(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Search vehicle options with pricing based on search criteria
     * 
     * POST /api/customer/intercity/vehicles
     */
    @PostMapping("/vehicles")
    public ResponseEntity<List<IntercityVehicleOptionDTO>> searchVehicleOptions(
            @RequestBody IntercityVehicleSearchRequest request
    ) {
        List<IntercityVehicleOptionDTO> options = pricingService.searchVehicleOptions(request);
        return ResponseEntity.ok(options);
    }
    
    /**
     * Get all vehicle options (simple GET - for backward compatibility)
     * 
     * GET /api/customer/intercity/vehicles/all
     */
    @GetMapping("/vehicles/all")
    public ResponseEntity<List<IntercityVehicleOptionDTO>> getAllVehicleOptions() {
        List<IntercityVehicleOptionDTO> options = pricingService.getVehicleOptions(null);
        return ResponseEntity.ok(options);
    }
    
    /**
     * Get details of a specific trip
     * 
     * GET /api/customer/intercity/trips/{tripId}
     */
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<IntercityTripDTO> getTripDetails(@PathVariable Long tripId) throws ResourceNotFoundException {
        var trip = tripService.getTripById(tripId);
        return ResponseEntity.ok(tripService.toDTO(trip));
    }
    
    /**
     * Get available trips for pooling by vehicle type
     * 
     * GET /api/customer/intercity/trips/available
     */
    @GetMapping("/trips/available")
    public ResponseEntity<List<IntercityTripDTO>> getAvailableTrips(
            @RequestParam IntercityVehicleType vehicleType,
            @RequestParam(defaultValue = "1") int seatsNeeded
    ) {
        var trips = tripService.findAvailableTripsForPooling(vehicleType, seatsNeeded);
        var dtos = trips.stream().map(tripService::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Create a new booking
     * 
     * POST /api/customer/intercity/bookings
     */
    @PostMapping("/bookings")
    public ResponseEntity<IntercityBookingResponse> createBooking(
            @AuthenticationPrincipal MyUser user,
            @Valid @RequestBody IntercityBookingRequest request
    ) throws ResourceNotFoundException {
        IntercityBookingResponse response = bookingService.createBooking(user.getId().toString(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * Confirm booking after payment
     * 
     * POST /api/customer/intercity/bookings/{bookingId}/confirm
     */
    @PostMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<IntercityBookingResponse> confirmBooking(
            @AuthenticationPrincipal MyUser user,
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> payload
    ) throws ResourceNotFoundException {
        String paymentId = payload.get("razorpayPaymentId");
        IntercityBookingResponse response = bookingService.confirmBooking(bookingId, paymentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel a booking
     * 
     * POST /api/customer/intercity/bookings/{bookingId}/cancel
     */
    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<IntercityBookingResponse> cancelBooking(
            @AuthenticationPrincipal MyUser user,
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> payload
    ) throws ResourceNotFoundException {
        String reason = payload != null ? payload.get("reason") : "Customer cancelled";
        IntercityBookingResponse response = bookingService.cancelBooking(bookingId, reason);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get booking details by ID
     * 
     * GET /api/customer/intercity/bookings/{bookingId}
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<IntercityBookingResponse> getBooking(
            @AuthenticationPrincipal MyUser user,
            @PathVariable Long bookingId
    ) throws ResourceNotFoundException {
        var booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }
    
    /**
     * Get booking by code
     * 
     * GET /api/customer/intercity/bookings/code/{bookingCode}
     */
    @GetMapping("/bookings/code/{bookingCode}")
    public ResponseEntity<IntercityBookingResponse> getBookingByCode(
            @AuthenticationPrincipal MyUser user,
            @PathVariable String bookingCode
    ) throws ResourceNotFoundException {
        var booking = bookingService.getBookingByCode(bookingCode);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }
    
    /**
     * Get user's booking history
     * 
     * GET /api/customer/intercity/bookings/history
     */
    @GetMapping("/bookings/history")
    public ResponseEntity<List<IntercityBookingResponse>> getBookingHistory(
            @AuthenticationPrincipal MyUser user
    ) {
        List<IntercityBookingResponse> bookings = bookingService.getUserBookings(user.getId().toString());
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get user's active bookings
     * 
     * GET /api/customer/intercity/bookings/active
     */
    @GetMapping("/bookings/active")
    public ResponseEntity<List<IntercityBookingResponse>> getActiveBookings(
            @AuthenticationPrincipal MyUser user
    ) {
        List<IntercityBookingResponse> bookings = bookingService.getUserActiveBookings(user.getId().toString());
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get alternative vehicle options for a trip
     * 
     * GET /api/customer/intercity/trips/{tripId}/alternatives
     */
    @GetMapping("/trips/{tripId}/alternatives")
    public ResponseEntity<List<IntercityAlternativeDTO>> getAlternatives(
            @PathVariable Long tripId
    ) throws ResourceNotFoundException {
        List<IntercityAlternativeDTO> alternatives = bookingService.getAlternatives(tripId);
        return ResponseEntity.ok(alternatives);
    }
    
    /**
     * Switch booking to an alternative vehicle
     * 
     * POST /api/customer/intercity/bookings/{bookingId}/switch
     */
    @PostMapping("/bookings/{bookingId}/switch")
    public ResponseEntity<IntercityBookingResponse> switchToAlternative(
            @AuthenticationPrincipal MyUser user,
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> payload
    ) throws ResourceNotFoundException {
        IntercityVehicleType newVehicleType = IntercityVehicleType.valueOf(payload.get("vehicleType"));
        IntercityBookingResponse response = bookingService.switchToAlternative(bookingId, newVehicleType);
        return ResponseEntity.ok(response);
    }
}

