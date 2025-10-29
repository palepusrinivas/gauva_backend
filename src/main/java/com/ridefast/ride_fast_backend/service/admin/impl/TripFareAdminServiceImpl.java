package com.ridefast.ride_fast_backend.service.admin.impl;

import com.ridefast.ride_fast_backend.dto.admin.TripFareUpsertRequest;
import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import com.ridefast.ride_fast_backend.model.v2.ZoneV2;
import com.ridefast.ride_fast_backend.repository.v2.TripFareRepository;
import com.ridefast.ride_fast_backend.repository.v2.VehicleCategoryRepository;
import com.ridefast.ride_fast_backend.repository.v2.ZoneV2Repository;
import com.ridefast.ride_fast_backend.service.admin.TripFareAdminService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripFareAdminServiceImpl implements TripFareAdminService {

  private final TripFareRepository tripFareRepository;
  private final ZoneV2Repository zoneRepository;
  private final VehicleCategoryRepository vehicleCategoryRepository;

  @Override
  public Page<TripFare> list(Pageable pageable) {
    return tripFareRepository.findAllByOrderByUpdatedAtDesc(pageable);
  }

  @Override
  public TripFare upsert(TripFareUpsertRequest req) {
    ZoneV2 zone = resolveZone(req);
    VehicleCategory category = resolveCategory(req);

    TripFare tf = tripFareRepository
        .findByZone_IdAndVehicleCategory_Id(zone.getId(), category.getId())
        .orElseGet(() -> TripFare.builder()
            .id(java.util.UUID.randomUUID().toString())
            .zone(zone)
            .vehicleCategory(category)
            .createdAt(LocalDateTime.now())
            .build());

    // Assign non-null values only (partial update allowed)
    if (req.getBaseFare() != null) tf.setBaseFare(req.getBaseFare());
    if (req.getBaseFarePerKm() != null) tf.setBaseFarePerKm(req.getBaseFarePerKm());
    if (req.getTimeRatePerMinOverride() != null) tf.setTimeRatePerMinOverride(req.getTimeRatePerMinOverride());
    if (req.getWaitingFeePerMin() != null) tf.setWaitingFeePerMin(req.getWaitingFeePerMin());
    if (req.getCancellationFeePercent() != null) tf.setCancellationFeePercent(req.getCancellationFeePercent());
    if (req.getMinCancellationFee() != null) tf.setMinCancellationFee(req.getMinCancellationFee());
    if (req.getIdleFeePerMin() != null) tf.setIdleFeePerMin(req.getIdleFeePerMin());
    if (req.getTripDelayFeePerMin() != null) tf.setTripDelayFeePerMin(req.getTripDelayFeePerMin());
    if (req.getPenaltyFeeForCancel() != null) tf.setPenaltyFeeForCancel(req.getPenaltyFeeForCancel());
    if (req.getFeeAddToNext() != null) tf.setFeeAddToNext(req.getFeeAddToNext());

    tf.setUpdatedAt(LocalDateTime.now());
    return tripFareRepository.save(tf);
  }

  @Override
  public void delete(String tripFareId) {
    tripFareRepository.deleteById(tripFareId);
  }

  private ZoneV2 resolveZone(TripFareUpsertRequest req) {
    if (req.getZoneId() != null && !req.getZoneId().isBlank()) {
      return zoneRepository.findById(req.getZoneId()).orElseThrow(() -> new IllegalArgumentException("Zone not found: " + req.getZoneId()));
    }
    if (req.getZoneName() != null && !req.getZoneName().isBlank()) {
      return zoneRepository.findByNameAndIsActiveTrue(req.getZoneName())
          .orElseThrow(() -> new IllegalArgumentException("Active Zone not found by name: " + req.getZoneName()));
    }
    throw new IllegalArgumentException("zoneId or zoneName is required");
  }

  private VehicleCategory resolveCategory(TripFareUpsertRequest req) {
    if (req.getVehicleCategoryId() != null && !req.getVehicleCategoryId().isBlank()) {
      return vehicleCategoryRepository.findById(req.getVehicleCategoryId())
          .orElseThrow(() -> new IllegalArgumentException("Vehicle category not found: " + req.getVehicleCategoryId()));
    }
    if (req.getCategoryType() != null && !req.getCategoryType().isBlank()) {
      Optional<VehicleCategory> byType = vehicleCategoryRepository.findByType(req.getCategoryType());
      if (byType.isPresent()) return byType.get();
    }
    if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
      Optional<VehicleCategory> byName = vehicleCategoryRepository.findByName(req.getCategoryName());
      if (byName.isPresent()) return byName.get();
    }
    throw new IllegalArgumentException("vehicleCategoryId or categoryType or categoryName is required");
  }
}
