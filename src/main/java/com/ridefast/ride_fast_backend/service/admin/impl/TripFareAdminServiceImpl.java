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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
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
    if (req == null) {
      throw new IllegalArgumentException("Request cannot be null");
    }
    
    log.debug("Upserting trip fare: zoneId={}, zoneName={}, categoryId={}, categoryType={}, categoryName={}", 
        req.getZoneId(), req.getZoneName(), req.getVehicleCategoryId(), req.getCategoryType(), req.getCategoryName());
    
    ZoneV2 zone = resolveZone(req);
    if (zone == null) {
      throw new IllegalArgumentException("Zone resolution failed");
    }
    
    VehicleCategory category = resolveCategory(req);
    if (category == null) {
      throw new IllegalArgumentException("Vehicle category resolution failed");
    }

    log.debug("Resolved zone: id={}, name={}", zone.getId(), zone.getName());
    log.debug("Resolved category: id={}, name={}, type={}", category.getId(), category.getName(), category.getType());

    TripFare tf = tripFareRepository
        .findByZone_IdAndVehicleCategory_Id(zone.getId(), category.getId())
        .orElseGet(() -> {
          log.debug("Creating new TripFare for zone={}, category={}", zone.getId(), category.getId());
          return TripFare.builder()
              .id(java.util.UUID.randomUUID().toString())
              .zone(zone)
              .vehicleCategory(category)
              .createdAt(LocalDateTime.now())
              .build();
        });

    // Assign non-null values only (partial update allowed)
    if (req.getBaseFare() != null) {
      tf.setBaseFare(req.getBaseFare());
      log.debug("Setting baseFare: {}", req.getBaseFare());
    }
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
    
    try {
      TripFare saved = tripFareRepository.save(tf);
      log.info("Trip fare saved successfully: id={}, zone={}, category={}", saved.getId(), zone.getName(), category.getName());
      return saved;
    } catch (Exception e) {
      log.error("Error saving trip fare: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to save trip fare: " + e.getMessage(), e);
    }
  }

  @Override
  public void delete(String tripFareId) {
    tripFareRepository.deleteById(tripFareId);
  }

  private ZoneV2 resolveZone(TripFareUpsertRequest req) {
    // Prefer zoneName over zoneId because:
    // 1. Old Zone model uses numeric IDs (1, 2, 3) but ZoneV2 uses UUID strings
    // 2. Zone names are consistent across both models
    // 3. Frontend may send numeric zoneId from old Zone model
    
    if (req.getZoneName() != null && !req.getZoneName().isBlank()) {
      log.debug("Resolving zone by name: {}", req.getZoneName());
      Optional<ZoneV2> zoneOpt = zoneRepository.findByNameAndIsActiveTrue(req.getZoneName());
      if (zoneOpt.isPresent()) {
        return zoneOpt.get();
      }
      log.warn("Active zone not found by name: {}", req.getZoneName());
      // Don't throw yet, try zoneId as fallback
    }
    
    if (req.getZoneId() != null && !req.getZoneId().isBlank()) {
      log.debug("Resolving zone by ID: {}", req.getZoneId());
      Optional<ZoneV2> zoneOpt = zoneRepository.findById(req.getZoneId());
      if (zoneOpt.isPresent()) {
        return zoneOpt.get();
      }
      log.warn("Zone not found by ID: {} (Note: ZoneV2 uses UUID strings, not numeric IDs)", req.getZoneId());
    }
    
    // If both zoneName and zoneId failed, throw error
    if (req.getZoneName() != null && !req.getZoneName().isBlank()) {
      throw new IllegalArgumentException("Active Zone not found by name: " + req.getZoneName() + 
          ". Please ensure the zone exists and is active in ZoneV2 (zones table).");
    }
    if (req.getZoneId() != null && !req.getZoneId().isBlank()) {
      throw new IllegalArgumentException("Zone not found: " + req.getZoneId() + 
          ". ZoneV2 uses UUID string IDs, not numeric IDs. Please use zoneName instead.");
    }
    
    log.error("Zone resolution failed: zoneId={}, zoneName={}", req.getZoneId(), req.getZoneName());
    throw new IllegalArgumentException("zoneId or zoneName is required");
  }

  private VehicleCategory resolveCategory(TripFareUpsertRequest req) {
    if (req.getVehicleCategoryId() != null && !req.getVehicleCategoryId().isBlank()) {
      log.debug("Resolving category by ID: {}", req.getVehicleCategoryId());
      Optional<VehicleCategory> catOpt = vehicleCategoryRepository.findById(req.getVehicleCategoryId());
      if (catOpt.isEmpty()) {
        log.warn("Vehicle category not found by ID: {}", req.getVehicleCategoryId());
        throw new IllegalArgumentException("Vehicle category not found: " + req.getVehicleCategoryId());
      }
      return catOpt.get();
    }
    if (req.getCategoryType() != null && !req.getCategoryType().isBlank()) {
      log.debug("Resolving category by type: {}", req.getCategoryType());
      Optional<VehicleCategory> byType = vehicleCategoryRepository.findByType(req.getCategoryType());
      if (byType.isPresent()) {
        log.debug("Found category by type: id={}, name={}", byType.get().getId(), byType.get().getName());
        return byType.get();
      }
      log.warn("Category not found by type: {}", req.getCategoryType());
    }
    if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
      log.debug("Resolving category by name: {}", req.getCategoryName());
      Optional<VehicleCategory> byName = vehicleCategoryRepository.findByName(req.getCategoryName());
      if (byName.isPresent()) {
        log.debug("Found category by name: id={}, type={}", byName.get().getId(), byName.get().getType());
        return byName.get();
      }
      log.warn("Category not found by name: {}", req.getCategoryName());
    }
    log.error("Category resolution failed: categoryId={}, categoryType={}, categoryName={}", 
        req.getVehicleCategoryId(), req.getCategoryType(), req.getCategoryName());
    throw new IllegalArgumentException("vehicleCategoryId or categoryType or categoryName is required");
  }
}
