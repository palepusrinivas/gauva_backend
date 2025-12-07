package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.Zone;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.ServiceConfigRepository;
import com.ridefast.ride_fast_backend.repository.ZoneRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/zones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminZonesController {

  private final ZoneRepository zoneRepository;
  private final DriverRepository driverRepository;
  private final ServiceConfigRepository serviceConfigRepository;

  @GetMapping
  public ResponseEntity<List<Zone>> list() {
    return ResponseEntity.ok(zoneRepository.findAll());
  }

  /**
   * Get operation zones with additional info (driver count, vehicle categories)
   * Used by Trip Fare Setup page
   */
  @GetMapping("/operation")
  public ResponseEntity<List<Map<String, Object>>> getOperationZones(
      @RequestParam(required = false) String search) {
    
    List<Zone> zones = zoneRepository.findAll();
    
    // Filter by search if provided
    if (search != null && !search.trim().isEmpty()) {
      String searchLower = search.toLowerCase().trim();
      zones = zones.stream()
          .filter(z -> (z.getName() != null && z.getName().toLowerCase().contains(searchLower)) ||
                       (z.getReadableId() != null && z.getReadableId().toLowerCase().contains(searchLower)))
          .toList();
    }

    // Get all active vehicle categories
    var vehicleCategories = serviceConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    
    // Build response with additional info
    List<Map<String, Object>> operationZones = new ArrayList<>();
    long totalDrivers = driverRepository.count();
    long driversPerZone = zones.isEmpty() ? 0 : totalDrivers / zones.size();
    
    for (Zone zone : zones) {
      Map<String, Object> opZone = new HashMap<>();
      opZone.put("id", zone.getId());
      opZone.put("name", zone.getName());
      opZone.put("readableId", zone.getReadableId());
      opZone.put("active", zone.getActive() != null ? zone.getActive() : zone.getIsActive());
      opZone.put("totalDrivers", driversPerZone); // Approximate distribution
      opZone.put("extraFareStatus", zone.getExtraFareStatus());
      opZone.put("extraFareFee", zone.getExtraFareFee());
      opZone.put("extraFareReason", zone.getExtraFareReason());
      opZone.put("createdAt", zone.getCreatedAt());
      opZone.put("updatedAt", zone.getUpdatedAt());
      
      // Add vehicle categories
      List<Map<String, Object>> categories = vehicleCategories.stream().map(vc -> {
        Map<String, Object> cat = new HashMap<>();
        cat.put("id", vc.getId());
        cat.put("name", vc.getName());
        cat.put("displayName", vc.getDisplayName());
        cat.put("icon", vc.getIcon());
        cat.put("baseFare", vc.getBaseFare());
        cat.put("perKmRate", vc.getPerKmRate());
        cat.put("perMinRate", vc.getPerMinRate());
        cat.put("minimumFare", vc.getMinimumFare());
        return cat;
      }).toList();
      opZone.put("vehicleCategories", categories);
      
      operationZones.add(opZone);
    }
    
    return ResponseEntity.ok(operationZones);
  }

  /**
   * Get single operation zone by ID
   */
  @GetMapping("/operation/{id}")
  public ResponseEntity<Map<String, Object>> getOperationZoneById(@PathVariable Long id) {
    return zoneRepository.findById(id)
        .map(zone -> {
          var vehicleCategories = serviceConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
          long totalDrivers = driverRepository.count();
          
          Map<String, Object> opZone = new HashMap<>();
          opZone.put("id", zone.getId());
          opZone.put("name", zone.getName());
          opZone.put("readableId", zone.getReadableId());
          opZone.put("active", zone.getActive() != null ? zone.getActive() : zone.getIsActive());
          opZone.put("totalDrivers", totalDrivers);
          opZone.put("extraFareStatus", zone.getExtraFareStatus());
          opZone.put("extraFareFee", zone.getExtraFareFee());
          opZone.put("extraFareReason", zone.getExtraFareReason());
          opZone.put("createdAt", zone.getCreatedAt());
          opZone.put("updatedAt", zone.getUpdatedAt());
          
          List<Map<String, Object>> categories = vehicleCategories.stream().map(vc -> {
            Map<String, Object> cat = new HashMap<>();
            cat.put("id", vc.getId());
            cat.put("name", vc.getName());
            cat.put("displayName", vc.getDisplayName());
            cat.put("icon", vc.getIcon());
            cat.put("baseFare", vc.getBaseFare());
            cat.put("perKmRate", vc.getPerKmRate());
            cat.put("perMinRate", vc.getPerMinRate());
            cat.put("minimumFare", vc.getMinimumFare());
            return cat;
          }).toList();
          opZone.put("vehicleCategories", categories);
          
          return ResponseEntity.ok(opZone);
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Zone> get(@PathVariable Long id) {
    Optional<Zone> z = zoneRepository.findById(id);
    return z.map(ResponseEntity::ok).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public ResponseEntity<Zone> create(@RequestBody Zone body) {
    if (body.getReadableId() == null || body.getReadableId().isBlank()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    // ID is auto-generated by the database, so don't set it
    body.setId(null);
    
    // Set defaults for required fields if not provided
    if (body.getExtraFareFee() == null) {
      body.setExtraFareFee(0.0);
    }
    if (body.getIsActive() == null) {
      body.setIsActive(body.getActive() != null ? body.getActive() : true);
    }
    if (body.getActive() == null) {
      body.setActive(body.getIsActive() != null ? body.getIsActive() : true);
    }
    Zone saved = zoneRepository.save(body);
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Zone> update(@PathVariable Long id, @RequestBody Zone body) {
    return zoneRepository.findById(id)
        .map(existing -> {
          if (body.getReadableId() != null) existing.setReadableId(body.getReadableId());
          if (body.getName() != null) existing.setName(body.getName());
          if (body.getPolygonWkt() != null) existing.setPolygonWkt(body.getPolygonWkt());
          if (body.getActive() != null) existing.setActive(body.getActive());
          return new ResponseEntity<>(zoneRepository.save(existing), HttpStatus.OK);
        })
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!zoneRepository.existsById(id)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    zoneRepository.deleteById(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
