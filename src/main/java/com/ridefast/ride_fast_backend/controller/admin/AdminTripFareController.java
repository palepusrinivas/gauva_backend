package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.dto.admin.TripFareUpsertRequest;
import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.service.admin.TripFareAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminTripFareController {

  private final TripFareAdminService tripFareAdminService;

  @GetMapping("/trip-fares")
  public ResponseEntity<Page<TripFare>> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(tripFareAdminService.list(pageable));
  }

  @PostMapping("/trip-fares")
  public ResponseEntity<?> upsert(@RequestBody TripFareUpsertRequest req) {
    try {
      log.info("Creating/updating trip fare: zoneId={}, zoneName={}, categoryId={}, categoryType={}, categoryName={}", 
          req.getZoneId(), req.getZoneName(), req.getVehicleCategoryId(), req.getCategoryType(), req.getCategoryName());
      
      TripFare saved = tripFareAdminService.upsert(req);
      log.info("Trip fare saved successfully: id={}", saved.getId());
      return new ResponseEntity<>(saved, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Invalid request for trip fare: {}", e.getMessage(), e);
      Map<String, Object> error = new HashMap<>();
      error.put("error", e.getMessage());
      error.put("message", "Validation failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    } catch (Exception e) {
      log.error("Error creating/updating trip fare: {}", e.getMessage(), e);
      Map<String, Object> error = new HashMap<>();
      error.put("error", "Failed to create/update trip fare");
      error.put("message", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  @DeleteMapping("/trip-fares")
  public ResponseEntity<Void> delete(@RequestParam("id") String id) {
    tripFareAdminService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
