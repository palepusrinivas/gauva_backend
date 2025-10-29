package com.ridefast.ride_fast_backend.service.admin.impl;

import com.ridefast.ride_fast_backend.dto.admin.AdminKpiResponse;
import com.ridefast.ride_fast_backend.dto.admin.AdminRecentTrip;
import com.ridefast.ride_fast_backend.dto.admin.AdminZoneStat;
import com.ridefast.ride_fast_backend.model.PaymentTransaction;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.repository.PaymentTransactionRepository;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.service.admin.AdminAnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

  private final RideRepository rideRepository;
  private final PaymentTransactionRepository paymentTransactionRepository;

  @Override
  public AdminKpiResponse getKpis(int windowDays) {
    LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
    List<Ride> recent = rideRepository.findByStartTimeBetween(since, LocalDateTime.now());

    Set<String> activeUserIds = recent.stream()
        .filter(r -> r.getUser() != null && r.getUser().getId() != null)
        .map(r -> r.getUser().getId())
        .collect(Collectors.toSet());

    Set<Long> activeDriverIds = recent.stream()
        .filter(r -> r.getDriver() != null && r.getDriver().getId() != null)
        .map(r -> r.getDriver().getId())
        .collect(Collectors.toSet());

    BigDecimal totalAllTime = paymentTransactionRepository.sumByStatus("PAID");
    BigDecimal total30d = paymentTransactionRepository.sumByStatusSince("PAID", since);

    return AdminKpiResponse.builder()
        .activeCustomers(activeUserIds.size())
        .activeDrivers(activeDriverIds.size())
        .totalEarningsAllTime(totalAllTime == null ? BigDecimal.ZERO : totalAllTime)
        .totalEarnings30d(total30d == null ? BigDecimal.ZERO : total30d)
        .build();
  }

  @Override
  public List<AdminRecentTrip> getRecentTrips(int limit) {
    return rideRepository.findAllByOrderByStartTimeDesc(PageRequest.of(0, limit))
        .getContent()
        .stream()
        .map(r -> AdminRecentTrip.builder()
            .rideId(r.getId())
            .userId(r.getUser() != null ? r.getUser().getId() : null)
            .driverId(r.getDriver() != null ? r.getDriver().getId() : null)
            .pickupArea(r.getPickupArea())
            .destinationArea(r.getDestinationArea())
            .fare(r.getFare())
            .duration(r.getDuration())
            .startTime(r.getStartTime())
            .endTime(r.getEndTime())
            .status(r.getStatus() != null ? r.getStatus().name() : null)
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public List<AdminZoneStat> getZoneStats(int windowDays) {
    LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
    List<Ride> recent = rideRepository.findByStartTimeBetween(since, LocalDateTime.now());

    Map<String, List<Ride>> byZone = new HashMap<>();
    for (Ride r : recent) {
      String zone = (r.getPickupArea() != null && !r.getPickupArea().isBlank()) ? r.getPickupArea() : "UNKNOWN";
      byZone.computeIfAbsent(zone, k -> new ArrayList<>()).add(r);
    }

    List<AdminZoneStat> stats = new ArrayList<>();
    for (Map.Entry<String, List<Ride>> e : byZone.entrySet()) {
      long trips = e.getValue().size();
      long totalDur = e.getValue().stream()
          .filter(r -> r.getDuration() != null)
          .mapToLong(Ride::getDuration)
          .sum();
      double avg = trips > 0 ? (double) totalDur / trips : 0.0;
      stats.add(AdminZoneStat.builder()
          .zone(e.getKey())
          .trips(trips)
          .totalDuration(totalDur)
          .avgDuration(avg)
          .build());
    }

    // sort desc by trips
    return stats.stream()
        .sorted(Comparator.comparingLong(AdminZoneStat::getTrips).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public List<PaymentTransaction> getRecentTransactions(int limit) {
    return paymentTransactionRepository
        .findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
        .getContent();
  }
}
