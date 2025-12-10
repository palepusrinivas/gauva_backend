package com.ridefast.ride_fast_backend.controller.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.*;
import com.ridefast.ride_fast_backend.enums.IntercityBookingStatus;
import com.ridefast.ride_fast_backend.enums.IntercityTripStatus;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.intercity.*;
import com.ridefast.ride_fast_backend.repository.intercity.*;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.intercity.IntercityBookingService;
import com.ridefast.ride_fast_backend.service.intercity.IntercityTripService;
import com.ridefast.ride_fast_backend.service.intercity.IntercityPricingConfigService;
import com.ridefast.ride_fast_backend.model.intercity.IntercityPricingConfig;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API for managing intercity bookings
 */
@RestController
@RequestMapping("/api/admin/intercity")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminIntercityController {
    
    private final IntercityBookingService bookingService;
    private final IntercityTripService tripService;
    private final IntercityPricingConfigService pricingConfigService;
    private final IntercityBookingRepository bookingRepository;
    private final IntercityTripRepository tripRepository;
    private final IntercityRouteRepository routeRepository;
    private final IntercityVehicleConfigRepository vehicleConfigRepository;
    private final DriverRepository driverRepository;
    
    // ==================== Dashboard ====================
    
    /**
     * Get dashboard statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();
        
        // Booking counts by status
        List<Object[]> bookingCounts = bookingRepository.countByStatus();
        Map<String, Long> bookingsByStatus = new HashMap<>();
        for (Object[] row : bookingCounts) {
            bookingsByStatus.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("bookingsByStatus", bookingsByStatus);
        
        // Trip counts by status
        List<Object[]> tripCounts = tripRepository.countByStatus();
        Map<String, Long> tripsByStatus = new HashMap<>();
        for (Object[] row : tripCounts) {
            tripsByStatus.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("tripsByStatus", tripsByStatus);
        
        // Vehicle configs
        stats.put("activeVehicleTypes", vehicleConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc().size());
        stats.put("activeRoutes", routeRepository.findByIsActiveTrue().size());
        
        return ResponseEntity.ok(stats);
    }
    
    // ==================== Vehicle Configuration ====================
    
    /**
     * Get all vehicle configurations
     */
    @GetMapping("/vehicles")
    public ResponseEntity<List<IntercityVehicleConfig>> getAllVehicleConfigs() {
        return ResponseEntity.ok(vehicleConfigRepository.findAll());
    }
    
    /**
     * Create or update vehicle configuration
     */
    @PostMapping("/vehicles")
    public ResponseEntity<IntercityVehicleConfig> saveVehicleConfig(
            @Valid @RequestBody VehicleConfigRequest request
    ) {
        IntercityVehicleConfig config = vehicleConfigRepository.findByVehicleType(request.getVehicleType())
            .orElse(new IntercityVehicleConfig());
        
        config.setVehicleType(request.getVehicleType());
        config.setDisplayName(request.getDisplayName());
        config.setTotalPrice(request.getTotalPrice());
        config.setMaxSeats(request.getMaxSeats());
        config.setMinSeats(request.getMinSeats());
        config.setDescription(request.getDescription());
        config.setTargetCustomer(request.getTargetCustomer());
        config.setRecommendationTag(request.getRecommendationTag());
        config.setDisplayOrder(request.getDisplayOrder());
        config.setIsActive(request.getIsActive());
        config.setImageUrl(request.getImageUrl());
        
        return new ResponseEntity<>(vehicleConfigRepository.save(config), HttpStatus.CREATED);
    }
    
    /**
     * Delete vehicle configuration
     */
    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicleConfig(@PathVariable Long id) {
        vehicleConfigRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== Route Management ====================
    
    /**
     * Get all routes
     */
    @GetMapping("/routes")
    public ResponseEntity<List<IntercityRoute>> getAllRoutes() {
        return ResponseEntity.ok(routeRepository.findAll());
    }
    
    /**
     * Create a new route
     */
    @PostMapping("/routes")
    public ResponseEntity<IntercityRoute> createRoute(
            @Valid @RequestBody RouteRequest request
    ) {
        IntercityRoute route = IntercityRoute.builder()
            .routeCode(request.getRouteCode())
            .originName(request.getOriginName())
            .originLatitude(request.getOriginLatitude())
            .originLongitude(request.getOriginLongitude())
            .destinationName(request.getDestinationName())
            .destinationLatitude(request.getDestinationLatitude())
            .destinationLongitude(request.getDestinationLongitude())
            .distanceKm(request.getDistanceKm())
            .durationMinutes(request.getDurationMinutes())
            .priceMultiplier(request.getPriceMultiplier())
            .isActive(request.getIsActive())
            .bidirectional(request.getBidirectional())
            .build();
        
        return new ResponseEntity<>(routeRepository.save(route), HttpStatus.CREATED);
    }
    
    /**
     * Update a route
     */
    @PutMapping("/routes/{id}")
    public ResponseEntity<IntercityRoute> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody RouteRequest request
    ) {
        IntercityRoute route = routeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Route not found"));
        
        route.setRouteCode(request.getRouteCode());
        route.setOriginName(request.getOriginName());
        route.setOriginLatitude(request.getOriginLatitude());
        route.setOriginLongitude(request.getOriginLongitude());
        route.setDestinationName(request.getDestinationName());
        route.setDestinationLatitude(request.getDestinationLatitude());
        route.setDestinationLongitude(request.getDestinationLongitude());
        route.setDistanceKm(request.getDistanceKm());
        route.setDurationMinutes(request.getDurationMinutes());
        route.setPriceMultiplier(request.getPriceMultiplier());
        route.setIsActive(request.getIsActive());
        route.setBidirectional(request.getBidirectional());
        
        return ResponseEntity.ok(routeRepository.save(route));
    }
    
    /**
     * Delete a route
     */
    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== Trip Management ====================
    
    /**
     * Get all trips with pagination
     */
    @GetMapping("/trips")
    public ResponseEntity<Page<IntercityTrip>> getAllTrips(
            @RequestParam(required = false) IntercityTripStatus status,
            Pageable pageable
    ) {
        Page<IntercityTrip> trips;
        if (status != null) {
            trips = tripRepository.findAll(pageable); // TODO: Add filter by status
        } else {
            trips = tripRepository.findAll(pageable);
        }
        return ResponseEntity.ok(trips);
    }
    
    /**
     * Get trips by status
     */
    @GetMapping("/trips/status/{status}")
    public ResponseEntity<List<IntercityTrip>> getTripsByStatus(
            @PathVariable IntercityTripStatus status
    ) {
        return ResponseEntity.ok(tripRepository.findByStatusOrderByScheduledDepartureAsc(status));
    }
    
    /**
     * Dispatch a trip
     */
    @PostMapping("/trips/{tripId}/dispatch")
    public ResponseEntity<Map<String, String>> dispatchTrip(@PathVariable Long tripId) throws ResourceNotFoundException {
        tripService.dispatchTrip(tripId);
        return ResponseEntity.ok(Map.of("status", "dispatched", "tripId", tripId.toString()));
    }
    
    /**
     * Cancel a trip
     */
    @PostMapping("/trips/{tripId}/cancel")
    public ResponseEntity<Map<String, String>> cancelTrip(
            @PathVariable Long tripId,
            @RequestBody Map<String, String> payload
    ) throws ResourceNotFoundException {
        String reason = payload.getOrDefault("reason", "Admin cancelled");
        tripService.cancelTrip(tripId, reason);
        return ResponseEntity.ok(Map.of("status", "cancelled", "tripId", tripId.toString()));
    }
    
    // ==================== Booking Management ====================
    
    /**
     * Get all bookings with pagination
     */
    @GetMapping("/bookings")
    public ResponseEntity<Page<IntercityBooking>> getAllBookings(
            @RequestParam(required = false) IntercityBookingStatus status,
            Pageable pageable
    ) {
        Page<IntercityBooking> bookings = bookingRepository.findAllWithFilters(status, pageable);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get booking details
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<IntercityBookingResponse> getBooking(@PathVariable Long bookingId) throws ResourceNotFoundException {
        var booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }
    
    /**
     * Admin confirm booking (move from HOLD to CONFIRMED)
     */
    @PostMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<IntercityBookingResponse> confirmBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> payload
    ) throws ResourceNotFoundException {
        String paymentMethodStr = payload != null ? payload.get("paymentMethod") : null;
        com.ridefast.ride_fast_backend.enums.IntercityPaymentMethod paymentMethod = 
            com.ridefast.ride_fast_backend.enums.IntercityPaymentMethod.ONLINE; // Default
        
        if (paymentMethodStr != null) {
            try {
                paymentMethod = com.ridefast.ride_fast_backend.enums.IntercityPaymentMethod.valueOf(
                    paymentMethodStr.toUpperCase()
                );
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment method: {}, defaulting to ONLINE", paymentMethodStr);
            }
        }
        
        IntercityBookingResponse response = bookingService.adminConfirmBooking(bookingId, paymentMethod);
        log.info("Admin confirmed booking {} with payment method {}", bookingId, paymentMethod);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get available drivers for assignment
     */
    @GetMapping("/drivers/available")
    public ResponseEntity<List<Map<String, Object>>> getAvailableDrivers(
            @RequestParam(required = false) String search
    ) {
        List<com.ridefast.ride_fast_backend.model.Driver> drivers;
        if (search != null && !search.trim().isEmpty()) {
            drivers = driverRepository.searchDrivers(search, org.springframework.data.domain.Pageable.unpaged()).getContent();
        } else {
            drivers = driverRepository.findAll();
        }
        
        List<Map<String, Object>> driverList = drivers.stream()
            .map(driver -> {
                Map<String, Object> driverMap = new HashMap<>();
                driverMap.put("id", driver.getId());
                driverMap.put("name", driver.getName());
                driverMap.put("email", driver.getEmail());
                driverMap.put("mobile", driver.getMobile());
                driverMap.put("shortCode", driver.getShortCode());
                driverMap.put("isOnline", driver.getIsOnline() != null ? driver.getIsOnline() : false);
                return driverMap;
            })
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(driverList);
    }
    
    /**
     * Admin assign driver to booking/trip
     */
    @PostMapping("/bookings/{bookingId}/assign-driver")
    public ResponseEntity<IntercityBookingResponse> assignDriver(
            @PathVariable Long bookingId,
            @RequestBody Map<String, Long> payload
    ) throws ResourceNotFoundException {
        Long driverId = payload.get("driverId");
        if (driverId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        IntercityBookingResponse response = bookingService.assignDriverToBooking(bookingId, driverId);
        log.info("Admin assigned driver {} to booking {}", driverId, bookingId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Admin cancel booking
     */
    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<IntercityBookingResponse> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> payload
    ) throws ResourceNotFoundException {
        String reason = payload.getOrDefault("reason", "Admin cancelled");
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, reason));
    }
    
    // ==================== Seed Data ====================
    
    /**
     * Seed default vehicle configurations
     */
    @PostMapping("/seed/vehicles")
    public ResponseEntity<Map<String, String>> seedVehicleConfigs() {
        // Car Premium Express
        if (!vehicleConfigRepository.existsByVehicleType(IntercityVehicleType.CAR_PREMIUM_EXPRESS)) {
            vehicleConfigRepository.save(IntercityVehicleConfig.builder()
                .vehicleType(IntercityVehicleType.CAR_PREMIUM_EXPRESS)
                .displayName("Car Premium Express")
                .totalPrice(new BigDecimal("1600"))
                .maxSeats(2)
                .minSeats(1)
                .description("Premium car with fast, comfortable travel")
                .targetCustomer("Professionals, business travellers")
                .recommendationTag("Fast & Comfort")
                .displayOrder(1)
                .isActive(true)
                .build());
        }
        
        // Car Normal
        if (!vehicleConfigRepository.existsByVehicleType(IntercityVehicleType.CAR_NORMAL)) {
            vehicleConfigRepository.save(IntercityVehicleConfig.builder()
                .vehicleType(IntercityVehicleType.CAR_NORMAL)
                .displayName("Car Normal Share")
                .totalPrice(new BigDecimal("1600"))
                .maxSeats(4)
                .minSeats(2)
                .description("Standard car with good comfort and value")
                .targetCustomer("Middle-class, families")
                .recommendationTag("Good Value")
                .displayOrder(2)
                .isActive(true)
                .build());
        }
        
        // Auto Normal
        if (!vehicleConfigRepository.existsByVehicleType(IntercityVehicleType.AUTO_NORMAL)) {
            vehicleConfigRepository.save(IntercityVehicleConfig.builder()
                .vehicleType(IntercityVehicleType.AUTO_NORMAL)
                .displayName("Auto Normal Pool")
                .totalPrice(new BigDecimal("1000"))
                .maxSeats(3)
                .minSeats(2)
                .description("Budget-friendly auto rickshaw")
                .targetCustomer("Budget travellers")
                .recommendationTag("Budget Friendly")
                .displayOrder(3)
                .isActive(true)
                .build());
        }
        
        // Tata Magic Lite
        if (!vehicleConfigRepository.existsByVehicleType(IntercityVehicleType.TATA_MAGIC_LITE)) {
            vehicleConfigRepository.save(IntercityVehicleConfig.builder()
                .vehicleType(IntercityVehicleType.TATA_MAGIC_LITE)
                .displayName("Tata Magic Lite")
                .totalPrice(new BigDecimal("1100"))
                .maxSeats(5)
                .minSeats(2)
                .description("Best value per head, fills quickly")
                .targetCustomer("Students, families, groups")
                .recommendationTag("Best Value")
                .displayOrder(4)
                .isActive(true)
                .build());
        }
        
        return ResponseEntity.ok(Map.of("status", "seeded", "message", "Default vehicle configs created"));
    }
    
    // ==================== Request DTOs ====================
    
    @Data
    public static class VehicleConfigRequest {
        private IntercityVehicleType vehicleType;
        private String displayName;
        private BigDecimal totalPrice;
        private Integer maxSeats;
        private Integer minSeats;
        private String description;
        private String targetCustomer;
        private String recommendationTag;
        private Integer displayOrder;
        private Boolean isActive;
        private String imageUrl;
    }
    
    @Data
    public static class RouteRequest {
        private String routeCode;
        private String originName;
        private Double originLatitude;
        private Double originLongitude;
        private String destinationName;
        private Double destinationLatitude;
        private Double destinationLongitude;
        private Double distanceKm;
        private Integer durationMinutes;
        private BigDecimal priceMultiplier;
        private Boolean isActive;
        private Boolean bidirectional;
    }
    
    // ==================== Pricing Configuration ====================
    
    /**
     * Get intercity pricing configuration
     * GET /api/admin/intercity/pricing
     */
    @GetMapping("/pricing")
    public ResponseEntity<IntercityPricingConfig> getPricingConfig() {
        IntercityPricingConfig config = pricingConfigService.getOrCreate();
        return ResponseEntity.ok(config);
    }
    
    /**
     * Update intercity pricing configuration
     * PUT /api/admin/intercity/pricing
     */
    @PutMapping("/pricing")
    public ResponseEntity<IntercityPricingConfig> updatePricingConfig(
            @Valid @RequestBody IntercityPricingConfigRequest request
    ) {
        IntercityPricingConfig updated = pricingConfigService.update(request);
        return ResponseEntity.ok(updated);
    }
}

