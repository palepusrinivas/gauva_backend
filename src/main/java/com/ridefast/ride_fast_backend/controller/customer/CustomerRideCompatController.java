package com.ridefast.ride_fast_backend.controller.customer;

import com.ridefast.ride_fast_backend.dto.FareEstimateRequest;
import com.ridefast.ride_fast_backend.dto.FareEstimateResponse;
import com.ridefast.ride_fast_backend.dto.LocationUpdate;
import com.ridefast.ride_fast_backend.service.FareEngine;
import com.ridefast.ride_fast_backend.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CustomerRideCompatController {
    @Autowired
  private final TrackingService trackingService;
  private final SimpMessagingTemplate messagingTemplate;
  private final FareEngine fareEngine;

  @PostMapping("/api/customer/ride/track-location")
  public ResponseEntity<?> trackLocation(
      @RequestParam(value = "ride_id", required = false) Long rideIdParam,
      @RequestParam(value = "trip_request_id", required = false) Long tripRequestIdParam,
      @RequestBody @Validated LocationUpdate update
  ) {
    if (update == null || update.getLat() == null || update.getLng() == null) {
      return new ResponseEntity<>(Map.of("error", "lat and lng are required"), HttpStatus.BAD_REQUEST);
    }
    Long rideId = rideIdParam != null ? rideIdParam : tripRequestIdParam;
    if (rideId == null) {
      return new ResponseEntity<>(Map.of("error", "ride_id or trip_request_id is required"), HttpStatus.BAD_REQUEST);
    }
    trackingService.saveLastLocation(rideId, update);
    messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/location", update);
    return ResponseEntity.ok(Map.of("status", "ok"));
  }

  @GetMapping("/api/user/get-live-location")
  public ResponseEntity<LocationUpdate> getLiveLocation(@RequestParam(name = "trip_request_id") Long rideId) {
    LocationUpdate lu = trackingService.getLastLocation(rideId);
    return ResponseEntity.ok(lu);
  }

  @PostMapping("/api/customer/ride/get-estimated-fare")
  public ResponseEntity<FareEstimateResponse> getEstimatedFare(@RequestBody FareEstimateRequest req) {
    FareEstimateResponse resp = fareEngine.estimate(req);
    return ResponseEntity.ok(resp);
  }
}
