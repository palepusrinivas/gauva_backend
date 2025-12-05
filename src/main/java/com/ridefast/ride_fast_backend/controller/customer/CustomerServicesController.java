package com.ridefast.ride_fast_backend.controller.customer;

import com.ridefast.ride_fast_backend.model.ServiceConfig;
import com.ridefast.ride_fast_backend.repository.ServiceConfigRepository;
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
}

