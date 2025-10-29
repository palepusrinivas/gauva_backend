package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.LocationUpdate;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.service.TrackingService;
import com.ridefast.ride_fast_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserLocationController {

  private final TrackingService trackingService;
  private final UserService userService;

  @PostMapping("/update-location")
  public ResponseEntity<?> updateMyLocation(
      @RequestHeader("Authorization") String jwtToken,
      @RequestBody @Validated LocationUpdate update
  ) throws ResourceNotFoundException, UserException {
    if (update == null || update.getLat() == null || update.getLng() == null) {
      return new ResponseEntity<>(Map.of("error", "lat and lng are required"), HttpStatus.BAD_REQUEST);
    }
    MyUser me = userService.getRequestedUserProfile(jwtToken);
    trackingService.saveUserLastLocation(me.getId(), update);
    return ResponseEntity.ok(Map.of("status", "ok"));
  }

  @GetMapping("/last-location")
  public ResponseEntity<LocationUpdate> getMyLastLocation(
      @RequestHeader("Authorization") String jwtToken
  ) throws ResourceNotFoundException, UserException {
    MyUser me = userService.getRequestedUserProfile(jwtToken);
    LocationUpdate lu = trackingService.getUserLastLocation(me.getId());
    return ResponseEntity.ok(lu);
  }
}
