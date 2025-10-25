package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.FareEstimateRequest;
import com.ridefast.ride_fast_backend.dto.FareEstimateResponse;
import com.ridefast.ride_fast_backend.service.FareEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fare")
@RequiredArgsConstructor
public class FareController {

  private final FareEngine fareEngine;

  @PostMapping("/estimate")
  public ResponseEntity<FareEstimateResponse> estimate(@RequestBody FareEstimateRequest req) {
    FareEstimateResponse resp = fareEngine.estimate(req);
    return ResponseEntity.ok(resp);
  }
}
