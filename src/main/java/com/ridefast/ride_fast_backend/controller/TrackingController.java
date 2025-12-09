package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.LocationUpdate;
import com.ridefast.ride_fast_backend.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.ridefast.ride_fast_backend.service.RealtimeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

  private final RealtimeService realtimeService;
  private final TrackingService trackingService;

  @PostMapping("/ride/{rideId}/location")
  public ResponseEntity<?> handleLocation(@PathVariable Long rideId, @RequestBody @Validated LocationUpdate update) {
    if (update == null || update.getLat() == null || update.getLng() == null) {
      return ResponseEntity.badRequest().body("Invalid location update");
    }
    // TODO: auth check: only assigned driver can publish for this ride
    trackingService.saveLastLocation(rideId, update);
    Double heading = update.getHeading() != null ? update.getHeading().doubleValue() : null;
    realtimeService.broadcastDriverLocation(rideId, null, update.getLat(), update.getLng(), heading);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/ride/{rideId}/last")
  public ResponseEntity<LocationUpdate> getLast(@PathVariable Long rideId) {
    LocationUpdate lu = trackingService.getLastLocation(rideId);
    return ResponseEntity.ok(lu);
  }
}
