package com.ridefast.ride_fast_backend.controller.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.*;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.intercity.IntercityRoute;
import com.ridefast.ride_fast_backend.model.intercity.IntercityVehicleConfig;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityRouteRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityVehicleConfigRepository;
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
    private final IntercityRouteRepository routeRepository;
    private final IntercityVehicleConfigRepository vehicleConfigRepository;
    
    /**
     * Get list of all active intercity routes/services
     * 
     * GET /api/customer/intercity/services
     * 
     * Returns all active intercity routes that customers can book
     */
    @GetMapping("/services")
    public ResponseEntity<List<IntercityRoute>> getIntercityServices() {
        List<IntercityRoute> routes = routeRepository.findByIsActiveTrue();
        return ResponseEntity.ok(routes);
    }
    
    /**
     * Get list of all active intercity vehicle/service types
     * 
     * GET /api/customer/intercity/service-types
     * 
     * Returns all active vehicle types (CAR_NORMAL, CAR_PREMIUM_EXPRESS, AUTO_NORMAL, TATA_MAGIC_LITE)
     * with their configuration details (pricing, seats, description, etc.)
     */
    @GetMapping("/service-types")
    public ResponseEntity<List<IntercityVehicleConfig>> getServiceTypes() {
        List<IntercityVehicleConfig> vehicleTypes = vehicleConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(vehicleTypes);
    }
    
    /**
     * Get a specific service type by vehicle type
     * 
     * GET /api/customer/intercity/service-types/{vehicleType}
     * 
     * Example: GET /api/customer/intercity/service-types/CAR_NORMAL
     */
    @GetMapping("/service-types/{vehicleType}")
    public ResponseEntity<IntercityVehicleConfig> getServiceType(
            @PathVariable String vehicleType
    ) {
        try {
            IntercityVehicleType type = IntercityVehicleType.valueOf(vehicleType.toUpperCase());
            return vehicleConfigRepository.findByVehicleType(type)
                    .filter(IntercityVehicleConfig::getIsActive)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get list of intercity routes by origin city
     * 
     * GET /api/customer/intercity/services/origin/{originName}
     */
    @GetMapping("/services/origin/{originName}")
    public ResponseEntity<List<IntercityRoute>> getServicesByOrigin(
            @PathVariable String originName
    ) {
        List<IntercityRoute> routes = routeRepository.findByOriginNameIgnoreCaseAndIsActiveTrue(originName);
        return ResponseEntity.ok(routes);
    }
    
    /**
     * Get list of intercity routes by destination city
     * 
     * GET /api/customer/intercity/services/destination/{destinationName}
     */
    @GetMapping("/services/destination/{destinationName}")
    public ResponseEntity<List<IntercityRoute>> getServicesByDestination(
            @PathVariable String destinationName
    ) {
        List<IntercityRoute> routes = routeRepository.findByDestinationNameIgnoreCaseAndIsActiveTrue(destinationName);
        return ResponseEntity.ok(routes);
    }
    
    /**
     * Get a specific route by route code
     * 
     * GET /api/customer/intercity/services/route/{routeCode}
     */
    @GetMapping("/services/route/{routeCode}")
    public ResponseEntity<IntercityRoute> getServiceByRouteCode(
            @PathVariable String routeCode
    ) {
        return routeRepository.findByRouteCode(routeCode)
                .filter(IntercityRoute::getIsActive)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
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
     * Search vehicle options with pricing (GET version - accepts query parameters)
     * 
     * GET /api/customer/intercity/vehicles
     * 
     * Query Parameters:
     * - routeId (optional): Route ID if known
     * - pickupLatitude (optional): Pickup latitude
     * - pickupLongitude (optional): Pickup longitude
     * - dropLatitude (optional): Drop latitude
     * - dropLongitude (optional): Drop longitude
     * - vehicleType (optional): Vehicle type filter (CAR_NORMAL, CAR_PREMIUM_EXPRESS, etc.)
     * - preferredDeparture (optional): Preferred departure time (ISO 8601 format)
     * - seatsNeeded (optional): Number of seats needed (default: 1)
     * - searchRadiusKm (optional): Search radius in kilometers (default: 5.0)
     */
    @GetMapping("/vehicles")
    public ResponseEntity<List<IntercityVehicleOptionDTO>> searchVehicleOptionsGet(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Double pickupLatitude,
            @RequestParam(required = false) Double pickupLongitude,
            @RequestParam(required = false) Double dropLatitude,
            @RequestParam(required = false) Double dropLongitude,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String preferredDeparture,
            @RequestParam(required = false, defaultValue = "1") Integer seatsNeeded,
            @RequestParam(required = false, defaultValue = "5.0") Double searchRadiusKm
    ) {
        // Build request from query parameters
        IntercityVehicleSearchRequest request = IntercityVehicleSearchRequest.builder()
                .routeId(routeId)
                .pickupLatitude(pickupLatitude)
                .pickupLongitude(pickupLongitude)
                .dropLatitude(dropLatitude)
                .dropLongitude(dropLongitude)
                .seatsNeeded(seatsNeeded)
                .searchRadiusKm(searchRadiusKm)
                .build();
        
        // Parse vehicle type if provided
        if (vehicleType != null && !vehicleType.isEmpty()) {
            try {
                request.setVehicleType(IntercityVehicleType.valueOf(vehicleType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid vehicle type: {}, ignoring filter", vehicleType);
            }
        }
        
        // Parse preferred departure if provided
        if (preferredDeparture != null && !preferredDeparture.isEmpty()) {
            try {
                request.setPreferredDeparture(java.time.LocalDateTime.parse(preferredDeparture));
            } catch (Exception e) {
                log.warn("Invalid preferred departure format: {}, ignoring", preferredDeparture);
            }
        }
        
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
        String paymentMethodStr = payload.get("paymentMethod");
        
        // Default to ONLINE if not specified
        com.ridefast.ride_fast_backend.enums.IntercityPaymentMethod paymentMethod = 
            com.ridefast.ride_fast_backend.enums.IntercityPaymentMethod.ONLINE;
        
        if (paymentMethodStr != null) {
            try {
                paymentMethod = com.ridefast.ride_fast_backend.enums.IntercityPaymentMethod.valueOf(
                    paymentMethodStr.toUpperCase()
                );
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment method: {}, defaulting to ONLINE", paymentMethodStr);
            }
        }
        
        IntercityBookingResponse response = bookingService.confirmBooking(bookingId, paymentId, paymentMethod);
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

