package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.dto.admin.TripFareUpsertRequest;
import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.service.admin.TripFareAdminService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTripFareController {

  private final TripFareAdminService tripFareAdminService;

  @GetMapping("/trip-fares")
  public ResponseEntity<Page<TripFare>> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(tripFareAdminService.list(pageable));
  }

  @PostMapping("/trip-fares")
  public ResponseEntity<TripFare> upsert(@RequestBody TripFareUpsertRequest req) {
    TripFare saved = tripFareAdminService.upsert(req);
    return new ResponseEntity<>(saved, HttpStatus.OK);
  }

  @DeleteMapping("/trip-fares")
  public ResponseEntity<Void> delete(@RequestParam("id") String id) {
    tripFareAdminService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
