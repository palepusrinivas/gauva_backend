package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.Vehicle;
import com.ridefast.ride_fast_backend.model.v2.VehicleV2;
import com.ridefast.ride_fast_backend.repository.VehicleRepository;
import com.ridefast.ride_fast_backend.repository.v2.VehicleV2Repository;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vehicle")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminVehicleController {

    private final VehicleRepository vehicleRepository;
    private final VehicleV2Repository vehicleV2Repository;

    /**
     * Get all vehicles
     * GET /api/admin/vehicle
     */
    @GetMapping
    public ResponseEntity<?> getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
            
            // Get vehicles from both repositories
            List<Map<String, Object>> vehicleList = new ArrayList<>();
            
            // Get old Vehicle entities
            Page<Vehicle> oldVehicles = vehicleRepository.findAll(pageable);
            for (Vehicle vehicle : oldVehicles.getContent()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", vehicle.getId());
                map.put("vehicleId", vehicle.getVehicleId());
                map.put("licensePlate", vehicle.getLicensePlate());
                map.put("vehicleBrand", vehicle.getCompany());
                map.put("vehicleModel", vehicle.getModel());
                map.put("vehicleCategory", "N/A"); // Old model doesn't have category
                map.put("fuelType", vehicle.getFuelType());
                map.put("status", "ACTIVE"); // Default status
                if (vehicle.getDriver() != null) {
                    map.put("driverId", vehicle.getDriver().getId());
                    // Driver model has name field
                    map.put("driverName", vehicle.getDriver().getName() != null ? vehicle.getDriver().getName() : "Unknown");
                }
                vehicleList.add(map);
            }
            
            // Get new VehicleV2 entities
            Page<VehicleV2> newVehicles = vehicleV2Repository.findAll(pageable);
            for (VehicleV2 vehicle : newVehicles.getContent()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", vehicle.getId());
                map.put("vehicleId", vehicle.getRefId());
                map.put("licensePlate", vehicle.getLicencePlateNumber());
                map.put("licenseExpiryDate", vehicle.getLicenceExpireDate() != null ? vehicle.getLicenceExpireDate().toString() : null);
                map.put("vinNumber", vehicle.getVinNumber());
                map.put("transmission", vehicle.getTransmission());
                map.put("fuelType", vehicle.getFuelType());
                map.put("ownership", vehicle.getOwnership());
                map.put("status", (vehicle.getIsActive() != null && vehicle.getIsActive()) ? "ACTIVE" : "INACTIVE");
                map.put("createdAt", vehicle.getCreatedAt() != null ? vehicle.getCreatedAt().toString() : null);
                
                // Brand info
                if (vehicle.getBrand() != null) {
                    map.put("vehicleBrand", vehicle.getBrand().getName());
                }
                
                // Model info
                if (vehicle.getModel() != null) {
                    map.put("vehicleModel", vehicle.getModel().getName());
                }
                
                // Category info
                if (vehicle.getCategory() != null) {
                    map.put("vehicleCategory", vehicle.getCategory().getName());
                }
                
                // Driver info
                if (vehicle.getDriver() != null) {
                    map.put("driverId", vehicle.getDriver().getId());
                    // MyUser has fullName, firstName, lastName - use fullName or construct from firstName + lastName
                    String driverName = vehicle.getDriver().getFullName();
                    if (driverName == null || driverName.isBlank()) {
                        String firstName = vehicle.getDriver().getFirstName() != null ? vehicle.getDriver().getFirstName() : "";
                        String lastName = vehicle.getDriver().getLastName() != null ? vehicle.getDriver().getLastName() : "";
                        driverName = (firstName + " " + lastName).trim();
                        if (driverName.isEmpty()) {
                            driverName = vehicle.getDriver().getEmail(); // Fallback to email
                        }
                    }
                    map.put("driverName", driverName);
                }
                
                vehicleList.add(map);
            }
            
            // Return as array for frontend compatibility
            // Frontend expects array directly, not paginated response
            return ResponseEntity.ok(vehicleList);
        } catch (Exception e) {
            log.error("Error fetching vehicles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch vehicles: " + e.getMessage()));
        }
    }

    /**
     * Get vehicle by ID
     * GET /api/admin/vehicle/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable String id) {
        try {
            // Try to find in VehicleV2 first (String ID)
            try {
                VehicleV2 vehicle = vehicleV2Repository.findById(id).orElse(null);
                if (vehicle != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", vehicle.getId());
                    map.put("vehicleId", vehicle.getRefId());
                    map.put("licensePlate", vehicle.getLicencePlateNumber());
                    map.put("licenseExpiryDate", vehicle.getLicenceExpireDate() != null ? vehicle.getLicenceExpireDate().toString() : null);
                    map.put("vinNumber", vehicle.getVinNumber());
                    map.put("transmission", vehicle.getTransmission());
                    map.put("fuelType", vehicle.getFuelType());
                    map.put("ownership", vehicle.getOwnership());
                    map.put("status", (vehicle.getIsActive() != null && vehicle.getIsActive()) ? "ACTIVE" : "INACTIVE");
                    
                    if (vehicle.getBrand() != null) {
                        map.put("vehicleBrand", vehicle.getBrand().getName());
                    }
                    if (vehicle.getModel() != null) {
                        map.put("vehicleModel", vehicle.getModel().getName());
                    }
                    if (vehicle.getCategory() != null) {
                        map.put("vehicleCategory", vehicle.getCategory().getName());
                    }
                    if (vehicle.getDriver() != null) {
                        map.put("driverId", vehicle.getDriver().getId());
                        // MyUser has fullName, firstName, lastName - use fullName or construct from firstName + lastName
                        String driverName = vehicle.getDriver().getFullName();
                        if (driverName == null || driverName.isBlank()) {
                            String firstName = vehicle.getDriver().getFirstName() != null ? vehicle.getDriver().getFirstName() : "";
                            String lastName = vehicle.getDriver().getLastName() != null ? vehicle.getDriver().getLastName() : "";
                            driverName = (firstName + " " + lastName).trim();
                            if (driverName.isEmpty()) {
                                driverName = vehicle.getDriver().getEmail(); // Fallback to email
                            }
                        }
                        map.put("driverName", driverName);
                    }
                    
                    return ResponseEntity.ok(map);
                }
            } catch (Exception e) {
                // Not a String ID, try Long
            }
            
            // Try to find in Vehicle (Long ID)
            try {
                Long longId = Long.parseLong(id);
                Vehicle vehicle = vehicleRepository.findById(longId).orElse(null);
                if (vehicle != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", vehicle.getId());
                    map.put("vehicleId", vehicle.getVehicleId());
                    map.put("licensePlate", vehicle.getLicensePlate());
                    map.put("vehicleBrand", vehicle.getCompany());
                    map.put("vehicleModel", vehicle.getModel());
                    map.put("fuelType", vehicle.getFuelType());
                    map.put("status", "ACTIVE");
                    if (vehicle.getDriver() != null) {
                        map.put("driverId", vehicle.getDriver().getId());
                        map.put("driverName", vehicle.getDriver().getName());
                    }
                    return ResponseEntity.ok(map);
                }
            } catch (NumberFormatException e) {
                // Not a valid Long ID
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Vehicle not found"));
        } catch (Exception e) {
            log.error("Error fetching vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch vehicle: " + e.getMessage()));
        }
    }
}

