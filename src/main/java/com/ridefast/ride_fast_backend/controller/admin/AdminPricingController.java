package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.dto.PricingConfigRequest;
import com.ridefast.ride_fast_backend.model.PricingConfig;
import com.ridefast.ride_fast_backend.service.PricingConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("adminPricingV1Controller")
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminPricingController {

  private final PricingConfigService service;

  @GetMapping
  public ResponseEntity<PricingConfig> get() {
    return ResponseEntity.ok(service.getOrCreate());
  }

  @PutMapping
  public ResponseEntity<PricingConfig> update(@RequestBody PricingConfigRequest request) {
    return ResponseEntity.ok(service.update(request));
  }
}
