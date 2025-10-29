package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.dto.admin.AdminKpiResponse;
import com.ridefast.ride_fast_backend.dto.admin.AdminRecentTrip;
import com.ridefast.ride_fast_backend.dto.admin.AdminZoneStat;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import com.ridefast.ride_fast_backend.service.admin.AdminAnalyticsService;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

  private final RideRepository rideRepository;
  private final AdminAnalyticsService adminAnalyticsService;

  @GetMapping("/heatmap")
  public ResponseEntity<?> heatmap(
      @RequestParam("from") String from,
      @RequestParam("to") String to) {
    try {
      LocalDateTime fromDt = LocalDateTime.parse(from);
      LocalDateTime toDt = LocalDateTime.parse(to);
      List<Ride> rides = rideRepository.findByStartTimeBetween(fromDt, toDt);
      List<Map<String, Object>> points = rides.stream()
          .filter(r -> r.getPickupLatitude() != null && r.getPickupLongitude() != null)
          .map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("lat", r.getPickupLatitude());
            m.put("lng", r.getPickupLongitude());
            m.put("weight", 1);
            return m;
          })
          .collect(Collectors.toList());
      return ResponseEntity.ok(points);
    } catch (DateTimeParseException ex) {
      return new ResponseEntity<>(Map.of("error", "Invalid datetime format. Use ISO-8601, e.g., 2025-10-25T00:00:00"), HttpStatus.BAD_REQUEST);
    }
  }

  @GetMapping("/kpis")
  public ResponseEntity<AdminKpiResponse> kpis(@RequestParam(defaultValue = "30") int windowDays) {
    return ResponseEntity.ok(adminAnalyticsService.getKpis(windowDays));
  }

  @GetMapping("/recent-trips")
  public ResponseEntity<List<AdminRecentTrip>> recentTrips(@RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(adminAnalyticsService.getRecentTrips(limit));
  }

  @GetMapping("/zones")
  public ResponseEntity<List<AdminZoneStat>> zones(@RequestParam(defaultValue = "7") int windowDays) {
    return ResponseEntity.ok(adminAnalyticsService.getZoneStats(windowDays));
  }

  @GetMapping("/recent-transactions")
  public ResponseEntity<List<PaymentTransaction>> recentTransactions(@RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(adminAnalyticsService.getRecentTransactions(limit));
  }
}
