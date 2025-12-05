package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.ServiceConfig;
import com.ridefast.ride_fast_backend.repository.ServiceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API for managing ride services (Car, Bike, Auto, etc.)
 */
@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminServiceConfigController {

    private final ServiceConfigRepository serviceConfigRepository;

    /**
     * Get all services with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<Page<ServiceConfig>> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "displayOrder"));

        Page<ServiceConfig> services;
        if (search != null && !search.trim().isEmpty()) {
            services = serviceConfigRepository.search(search.trim(), pageable);
        } else if (active != null) {
            services = serviceConfigRepository.findByIsActive(active, pageable);
        } else {
            services = serviceConfigRepository.findAll(pageable);
        }

        return ResponseEntity.ok(services);
    }

    /**
     * Get all services as a simple list
     */
    @GetMapping("/list")
    public ResponseEntity<List<ServiceConfig>> getServicesList(
            @RequestParam(required = false) Boolean active) {

        List<ServiceConfig> services;
        if (active != null && active) {
            services = serviceConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        } else {
            services = serviceConfigRepository.findAllByOrderByDisplayOrderAsc();
        }

        return ResponseEntity.ok(services);
    }

    /**
     * Get service by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceConfig> getServiceById(@PathVariable Long id) {
        return serviceConfigRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get service by service ID (e.g., "BIKE", "CAR")
     */
    @GetMapping("/by-code/{serviceId}")
    public ResponseEntity<ServiceConfig> getServiceByServiceId(@PathVariable String serviceId) {
        return serviceConfigRepository.findByServiceId(serviceId.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new service
     */
    @PostMapping
    public ResponseEntity<?> createService(@RequestBody ServiceConfig request) {
        try {
            // Validate required fields
            if (request.getServiceId() == null || request.getServiceId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Service ID is required"));
            }
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }

            // Normalize service ID
            request.setServiceId(request.getServiceId().toUpperCase().trim().replace(" ", "_"));

            // Check if service ID already exists
            if (serviceConfigRepository.existsByServiceId(request.getServiceId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Service ID already exists"));
            }

            // Set defaults
            if (request.getDisplayName() == null) {
                request.setDisplayName(request.getName());
            }
            if (request.getIsActive() == null) {
                request.setIsActive(true);
            }
            if (request.getCapacity() == null) {
                request.setCapacity(1);
            }
            if (request.getDisplayOrder() == null) {
                request.setDisplayOrder(0);
            }

            ServiceConfig saved = serviceConfigRepository.save(request);
            log.info("Service created: id={}, serviceId={}", saved.getId(), saved.getServiceId());

            return new ResponseEntity<>(saved, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Error creating service", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create service: " + e.getMessage()));
        }
    }

    /**
     * Update service
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody ServiceConfig request) {
        return serviceConfigRepository.findById(id)
                .map(existing -> {
                    // Update fields if provided
                    if (request.getName() != null) existing.setName(request.getName());
                    if (request.getDisplayName() != null) existing.setDisplayName(request.getDisplayName());
                    if (request.getDescription() != null) existing.setDescription(request.getDescription());
                    if (request.getIcon() != null) existing.setIcon(request.getIcon());
                    if (request.getIconUrl() != null) existing.setIconUrl(request.getIconUrl());
                    if (request.getCapacity() != null) existing.setCapacity(request.getCapacity());
                    if (request.getDisplayOrder() != null) existing.setDisplayOrder(request.getDisplayOrder());
                    if (request.getIsActive() != null) existing.setIsActive(request.getIsActive());
                    if (request.getVehicleType() != null) existing.setVehicleType(request.getVehicleType());
                    if (request.getEstimatedArrival() != null) existing.setEstimatedArrival(request.getEstimatedArrival());
                    if (request.getBaseFare() != null) existing.setBaseFare(request.getBaseFare());
                    if (request.getPerKmRate() != null) existing.setPerKmRate(request.getPerKmRate());
                    if (request.getPerMinRate() != null) existing.setPerMinRate(request.getPerMinRate());
                    if (request.getMinimumFare() != null) existing.setMinimumFare(request.getMinimumFare());
                    if (request.getCancellationFee() != null) existing.setCancellationFee(request.getCancellationFee());
                    if (request.getMaxDistance() != null) existing.setMaxDistance(request.getMaxDistance());
                    if (request.getMaxWaitTime() != null) existing.setMaxWaitTime(request.getMaxWaitTime());
                    if (request.getCategory() != null) existing.setCategory(request.getCategory());

                    ServiceConfig updated = serviceConfigRepository.save(existing);
                    log.info("Service updated: id={}", updated.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Toggle service active status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleServiceStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {

        return serviceConfigRepository.findById(id)
                .map(service -> {
                    service.setIsActive(active);
                    ServiceConfig updated = serviceConfigRepository.save(service);
                    log.info("Service status updated: id={}, active={}", id, active);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update display order for multiple services
     */
    @PutMapping("/reorder")
    public ResponseEntity<?> reorderServices(@RequestBody List<Map<String, Object>> orderList) {
        try {
            for (Map<String, Object> item : orderList) {
                Long id = Long.valueOf(item.get("id").toString());
                Integer order = Integer.valueOf(item.get("displayOrder").toString());

                serviceConfigRepository.findById(id).ifPresent(service -> {
                    service.setDisplayOrder(order);
                    serviceConfigRepository.save(service);
                });
            }
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Order updated successfully"));
        } catch (Exception e) {
            log.error("Error reordering services", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to reorder: " + e.getMessage()));
        }
    }

    /**
     * Delete service
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        return serviceConfigRepository.findById(id)
                .map(service -> {
                    serviceConfigRepository.delete(service);
                    log.info("Service deleted: id={}, serviceId={}", id, service.getServiceId());
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get service statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getServiceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", serviceConfigRepository.count());
        stats.put("active", serviceConfigRepository.countByIsActiveTrue());
        stats.put("inactive", serviceConfigRepository.countByIsActiveFalse());
        return ResponseEntity.ok(stats);
    }

    /**
     * Seed default services
     */
    @PostMapping("/seed-defaults")
    public ResponseEntity<?> seedDefaultServices() {
        try {
            List<ServiceConfig> defaults = List.of(
                    ServiceConfig.builder()
                            .serviceId("BIKE")
                            .name("Bike")
                            .displayName("Bike Taxi")
                            .description("Quick and affordable bike rides")
                            .icon("🏍️")
                            .capacity(1)
                            .displayOrder(1)
                            .isActive(true)
                            .vehicleType("two_wheeler")
                            .estimatedArrival("2-5 mins")
                            .category("economy")
                            .baseFare(20.0)
                            .perKmRate(8.0)
                            .perMinRate(1.0)
                            .minimumFare(25.0)
                            .build(),

                    ServiceConfig.builder()
                            .serviceId("AUTO")
                            .name("Auto")
                            .displayName("Auto Rickshaw")
                            .description("Classic auto rickshaw rides")
                            .icon("🛺")
                            .capacity(3)
                            .displayOrder(2)
                            .isActive(true)
                            .vehicleType("three_wheeler")
                            .estimatedArrival("3-7 mins")
                            .category("economy")
                            .baseFare(30.0)
                            .perKmRate(12.0)
                            .perMinRate(1.5)
                            .minimumFare(35.0)
                            .build(),

                    ServiceConfig.builder()
                            .serviceId("CAR")
                            .name("Car")
                            .displayName("Car")
                            .description("Comfortable car rides")
                            .icon("🚗")
                            .capacity(4)
                            .displayOrder(3)
                            .isActive(true)
                            .vehicleType("four_wheeler")
                            .estimatedArrival("5-10 mins")
                            .category("standard")
                            .baseFare(50.0)
                            .perKmRate(15.0)
                            .perMinRate(2.0)
                            .minimumFare(60.0)
                            .build(),

                    ServiceConfig.builder()
                            .serviceId("PREMIUM")
                            .name("Premium")
                            .displayName("Premium Car")
                            .description("Premium and luxury car rides")
                            .icon("🚘")
                            .capacity(4)
                            .displayOrder(4)
                            .isActive(true)
                            .vehicleType("four_wheeler_premium")
                            .estimatedArrival("8-15 mins")
                            .category("premium")
                            .baseFare(100.0)
                            .perKmRate(25.0)
                            .perMinRate(3.0)
                            .minimumFare(120.0)
                            .build()
            );

            int created = 0;
            for (ServiceConfig config : defaults) {
                if (!serviceConfigRepository.existsByServiceId(config.getServiceId())) {
                    serviceConfigRepository.save(config);
                    created++;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "created", created,
                    "skipped", defaults.size() - created
            ));

        } catch (Exception e) {
            log.error("Error seeding defaults", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to seed defaults: " + e.getMessage()));
        }
    }
}

