package com.ridefast.ride_fast_backend.controller.customer;

import com.ridefast.ride_fast_backend.dto.FareEstimateRequest;
import com.ridefast.ride_fast_backend.dto.FareEstimateResponse;
import com.ridefast.ride_fast_backend.enums.ServiceType;
import com.ridefast.ride_fast_backend.model.ServiceConfig;
import com.ridefast.ride_fast_backend.model.v2.ZoneV2;
import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.repository.ServiceConfigRepository;
import com.ridefast.ride_fast_backend.repository.v2.ZoneV2Repository;
import com.ridefast.ride_fast_backend.repository.v2.VehicleCategoryRepository;
import com.ridefast.ride_fast_backend.repository.v2.TripFareRepository;
import com.ridefast.ride_fast_backend.service.FareEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Public API for fetching available ride services (Car, Bike, Auto, etc.)
 * Used by mobile apps to display service options
 * Data is managed by admin via /api/admin/services
 */
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class CustomerServicesController {

    private final ServiceConfigRepository serviceConfigRepository;
    private final FareEngine fareEngine;
    private final ZoneV2Repository zoneRepository;
    private final VehicleCategoryRepository vehicleCategoryRepository;
    private final TripFareRepository tripFareRepository;

    /**
     * Get all active services with their details
     * This is the main endpoint for Flutter app to get service options
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllServices() {
        List<ServiceConfig> services = serviceConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        
        Map<String, Object> response = new HashMap<>();
        response.put("services", services);
        response.put("total", services.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get active services as a simple list
     */
    @GetMapping("/list")
    public ResponseEntity<List<ServiceConfig>> getServicesList() {
        List<ServiceConfig> services = serviceConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(services);
    }

    /**
     * Get a specific service by ID
     */
    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceConfig> getServiceById(@PathVariable String serviceId) {
        return serviceConfigRepository.findByServiceId(serviceId.toUpperCase())
                .filter(ServiceConfig::getIsActive)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get services available at a specific location
     * Currently returns all active services, can be extended for zone-based filtering
     */
    @GetMapping("/available")
    public ResponseEntity<List<ServiceConfig>> getAvailableServices(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String zoneId) {
        
        // For now, return all active services
        // TODO: Filter by zone availability based on lat/lng or zoneId
        List<ServiceConfig> services = serviceConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(services);
    }

    /**
     * Get services by category (economy, standard, premium)
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ServiceConfig>> getServicesByCategory(@PathVariable String category) {
        List<ServiceConfig> services = serviceConfigRepository.findByCategory(category.toLowerCase());
        // Filter only active ones
        services = services.stream().filter(ServiceConfig::getIsActive).toList();
        return ResponseEntity.ok(services);
    }

    /**
     * Get services by vehicle type (two_wheeler, three_wheeler, four_wheeler)
     */
    @GetMapping("/vehicle-type/{vehicleType}")
    public ResponseEntity<List<ServiceConfig>> getServicesByVehicleType(@PathVariable String vehicleType) {
        List<ServiceConfig> services = serviceConfigRepository.findByVehicleType(vehicleType);
        services = services.stream().filter(ServiceConfig::getIsActive).toList();
        return ResponseEntity.ok(services);
    }

    /**
     * Get fare estimates for ALL active services at once
     * This is useful for the ride booking screen to show all options with fares
     * Returns estimates for all active services configured in the database
     * 
     * POST /api/v1/services/fare-estimates
     */
    @PostMapping("/fare-estimates")
    public ResponseEntity<List<FareEstimateResponse>> getAllFareEstimates(
            @RequestBody FareEstimateRequest baseRequest) {
        
        List<FareEstimateResponse> estimates = new ArrayList<>();
        
        // Get all active services from database (not just enum values)
        List<ServiceConfig> activeServices = serviceConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        
        for (ServiceConfig service : activeServices) {
            try {
                FareEstimateResponse estimate = null;
                
                // Try to use ServiceType enum if serviceId matches
                try {
                    ServiceType serviceType = ServiceType.valueOf(service.getServiceId().toUpperCase());
                    FareEstimateRequest req = new FareEstimateRequest();
                    req.setServiceType(serviceType);
                    req.setDistanceKm(baseRequest.getDistanceKm());
                    req.setDurationMin(baseRequest.getDurationMin());
                    req.setPickupLat(baseRequest.getPickupLat());
                    req.setPickupLng(baseRequest.getPickupLng());
                    req.setDropLat(baseRequest.getDropLat());
                    req.setDropLng(baseRequest.getDropLng());
                    req.setPickupZoneReadableId(baseRequest.getPickupZoneReadableId());
                    req.setDropZoneReadableId(baseRequest.getDropZoneReadableId());
                    req.setCouponCode(baseRequest.getCouponCode());
                    req.setUserId(baseRequest.getUserId());
                    
                    estimate = fareEngine.estimate(req);
                } catch (IllegalArgumentException e) {
                    // Service ID doesn't match enum - calculate fare using ServiceConfig directly
                    estimate = calculateFareFromServiceConfig(service, baseRequest);
                }
                
                if (estimate != null) {
                    estimates.add(estimate);
                }
            } catch (Exception e) {
                // Skip if fare calculation fails for this service
                // Log error for debugging
                System.err.println("Failed to calculate fare for service " + service.getServiceId() + ": " + e.getMessage());
            }
        }
        
        // Sort by final total (cheapest first)
        estimates.sort(Comparator.comparingDouble(FareEstimateResponse::getFinalTotal));
        
        return ResponseEntity.ok(estimates);
    }
    
    /**
     * Calculate fare estimate using ServiceConfig directly (for services not in ServiceType enum)
     * Also applies zone-specific trip fares if available
     */
    private FareEstimateResponse calculateFareFromServiceConfig(ServiceConfig service, FareEstimateRequest baseRequest) {
        // Start with fare rates from ServiceConfig
        double baseFare = service.getBaseFare() != null ? service.getBaseFare() : 0.0;
        double perKmRate = service.getPerKmRate() != null ? service.getPerKmRate() : 0.0;
        double perMinRate = service.getPerMinRate() != null ? service.getPerMinRate() : 0.0;
        
        // Apply zone-specific trip fare overrides if available
        ZoneV2 pickupZone = null;
        if (baseRequest.getPickupZoneReadableId() != null) {
            pickupZone = zoneRepository.findByNameAndIsActiveTrue(baseRequest.getPickupZoneReadableId()).orElse(null);
        }
        ZoneV2 dropZone = null;
        if (pickupZone == null && baseRequest.getDropZoneReadableId() != null) {
            dropZone = zoneRepository.findByNameAndIsActiveTrue(baseRequest.getDropZoneReadableId()).orElse(null);
        }
        
        ZoneV2 zoneForOverride = pickupZone != null ? pickupZone : dropZone;
        if (zoneForOverride != null) {
            // Try to find vehicle category by serviceId or vehicleType
            Optional<VehicleCategory> catOpt = vehicleCategoryRepository.findByType(service.getServiceId());
            if (catOpt.isEmpty()) {
                catOpt = vehicleCategoryRepository.findByName(service.getServiceId());
            }
            if (catOpt.isEmpty() && service.getVehicleType() != null) {
                catOpt = vehicleCategoryRepository.findByType(service.getVehicleType().toUpperCase());
            }
            
            if (catOpt.isPresent()) {
                Optional<TripFare> tfOpt = tripFareRepository.findFirstByZoneAndVehicleCategory(zoneForOverride, catOpt.get());
                if (tfOpt.isPresent()) {
                    TripFare tf = tfOpt.get();
                    if (tf.getBaseFare() != null) baseFare = tf.getBaseFare().doubleValue();
                    if (tf.getBaseFarePerKm() != null) perKmRate = tf.getBaseFarePerKm().doubleValue();
                    if (tf.getTimeRatePerMinOverride() != null) perMinRate = tf.getTimeRatePerMinOverride().doubleValue();
                }
            }
        }
        
        // Calculate fare components
        double distanceFare = round2(baseRequest.getDistanceKm() * perKmRate);
        double timeFare = round2(baseRequest.getDurationMin() * perMinRate);
        double total = round2(baseFare + distanceFare + timeFare);
        
        // Apply minimum fare if configured
        if (service.getMinimumFare() != null && service.getMinimumFare() > 0 && total < service.getMinimumFare()) {
            total = service.getMinimumFare();
        }
        
        // Build vehicle info from ServiceConfig
        FareEstimateResponse.VehicleInfo vehicleInfo = FareEstimateResponse.VehicleInfo.builder()
                .serviceId(service.getServiceId())
                .name(service.getName())
                .displayName(service.getDisplayName() != null ? service.getDisplayName() : service.getName())
                .icon(service.getIcon())
                .iconUrl(service.getIconUrl())
                .capacity(service.getCapacity() != null ? service.getCapacity() : 1)
                .vehicleType(service.getVehicleType())
                .category(service.getCategory())
                .estimatedArrival(service.getEstimatedArrival())
                .description(service.getDescription())
                .build();
        
        return FareEstimateResponse.builder()
                .currency("INR") // Default currency
                .baseFare(baseFare)
                .distanceKm(baseRequest.getDistanceKm())
                .perKmRate(perKmRate)
                .distanceFare(distanceFare)
                .durationMin(baseRequest.getDurationMin())
                .timeRatePerMin(perMinRate)
                .timeFare(timeFare)
                .cancellationFee(service.getCancellationFee() != null ? service.getCancellationFee() : 0.0)
                .returnFee(0.0)
                .total(total)
                .discount(0.0) // Coupon discount can be applied later if needed
                .finalTotal(total)
                .appliedCoupon(null)
                .extraFareStatus(com.ridefast.ride_fast_backend.enums.ExtraFareStatus.NONE)
                .extraFareReason(null)
                .vehicle(vehicleInfo)
                .build();
    }
    
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Get fare estimate for a specific service type with full vehicle info
     * 
     * POST /api/v1/services/{serviceId}/fare
     */
    @PostMapping("/{serviceId}/fare")
    public ResponseEntity<FareEstimateResponse> getServiceFare(
            @PathVariable String serviceId,
            @RequestBody FareEstimateRequest request) {
        
        try {
            ServiceType type = ServiceType.valueOf(serviceId.toUpperCase());
            request.setServiceType(type);
            FareEstimateResponse estimate = fareEngine.estimate(request);
            return ResponseEntity.ok(estimate);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

