package com.ridefast.ride_fast_backend.controller.customer;

import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.enums.RideStatus;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.CalculatorService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerDriverController {

  private final DriverRepository driverRepository;
  private final CalculatorService calculatorService;
  private final ModelMapper modelMapper;

  @GetMapping("/drivers-near-me")
  public ResponseEntity<List<DriverResponse>> driversNearMe(
      @RequestParam("lat") double lat,
      @RequestParam("lng") double lng,
      @RequestParam(value = "radius_m", required = false) Integer radiusMeters,
      @RequestParam(value = "limit", required = false) Integer limit
  ) {
    double radiusKm = radiusMeters != null ? (radiusMeters / 1000.0) : 5.0; // default 5km

    List<DriverResponse> nearby = driverRepository.findAll().stream()
        .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
        .filter(d -> d.getCurrentRide() == null || d.getCurrentRide().getStatus() == RideStatus.COMPLETED)
        .map(d -> new Object[]{d, calculatorService.calculateDistance(d.getLatitude(), d.getLongitude(), lat, lng)})
        .filter(arr -> (double) arr[1] <= radiusKm)
        .sorted(Comparator.comparingDouble(arr -> (double) arr[1]))
        .map(arr -> (Driver) arr[0])
        .map(d -> modelMapper.map(d, DriverResponse.class))
        .collect(Collectors.toList());

    if (limit != null && limit > 0 && nearby.size() > limit) {
      nearby = nearby.subList(0, limit);
    }

    return ResponseEntity.ok(nearby);
  }
}
