package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.LocationUpdate;
import com.ridefast.ride_fast_backend.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

  private final SimpMessagingTemplate messagingTemplate;
  private final TrackingService trackingService;

  @MessageMapping("/ride/{rideId}/location")
  public void handleLocation(@DestinationVariable Long rideId, @Validated LocationUpdate update) {
    if (update == null || update.getLat() == null || update.getLng() == null) return;
    // TODO: auth check: only assigned driver can publish for this ride
    trackingService.saveLastLocation(rideId, update);
    messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/location", update);
  }

  @GetMapping("/ride/{rideId}/last")
  public ResponseEntity<LocationUpdate> getLast(@PathVariable Long rideId) {
    LocationUpdate lu = trackingService.getLastLocation(rideId);
    return ResponseEntity.ok(lu);
  }
}
