package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.repository.DriverRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/fleet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFleetController {

  private final DriverRepository driverRepository;

  @GetMapping("/locations")
  public ResponseEntity<List<Map<String, Object>>> listLocations() {
    List<Map<String, Object>> markers = driverRepository.findAll().stream()
        .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
        .map(d -> Map.of(
            "driverId", d.getId(),
            "title", d.getName() == null ? ("Driver " + d.getId()) : d.getName(),
            "subtitle", d.getMobile(),
            "position", Map.of("lat", d.getLatitude(), "lng", d.getLongitude())
        ))
        .collect(Collectors.toList());
    return ResponseEntity.ok(markers);
  }
}
