package com.ridefast.ride_fast_backend.service.admin.impl;

import com.ridefast.ride_fast_backend.dto.admin.TripFareUpsertRequest;
import com.ridefast.ride_fast_backend.model.Zone;
import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import com.ridefast.ride_fast_backend.model.v2.ZoneV2;
import com.ridefast.ride_fast_backend.repository.ZoneRepository;
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
  private final ZoneV2Repository zoneV2Repository;
  private final ZoneRepository zoneRepository; // Old Zone table
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
          // Create with default values for required fields (all are nullable=false in database)
          return TripFare.builder()
              .id(java.util.UUID.randomUUID().toString())
              .zone(zone)
              .vehicleCategory(category)
              .baseFare(java.math.BigDecimal.ZERO)
              .baseFarePerKm(java.math.BigDecimal.ZERO)
              .waitingFeePerMin(java.math.BigDecimal.ZERO)
              .cancellationFeePercent(java.math.BigDecimal.ZERO)
              .minCancellationFee(java.math.BigDecimal.ZERO)
              .idleFeePerMin(java.math.BigDecimal.ZERO)
              .tripDelayFeePerMin(java.math.BigDecimal.ZERO)
              .penaltyFeeForCancel(java.math.BigDecimal.ZERO)
              .feeAddToNext(java.math.BigDecimal.ZERO)
              .createdAt(LocalDateTime.now())
              .build();
        });

    // Assign non-null values from request (partial update allowed)
    // For new TripFare, these will override the default zeros
    // For existing TripFare, these will update only if provided
    if (req.getBaseFare() != null) {
      tf.setBaseFare(req.getBaseFare());
      log.debug("Setting baseFare: {}", req.getBaseFare());
    }
    if (req.getBaseFarePerKm() != null) {
      tf.setBaseFarePerKm(req.getBaseFarePerKm());
    }
    if (req.getTimeRatePerMinOverride() != null) {
      tf.setTimeRatePerMinOverride(req.getTimeRatePerMinOverride());
    }
    if (req.getWaitingFeePerMin() != null) {
      tf.setWaitingFeePerMin(req.getWaitingFeePerMin());
    }
    if (req.getCancellationFeePercent() != null) {
      tf.setCancellationFeePercent(req.getCancellationFeePercent());
    }
    if (req.getMinCancellationFee() != null) {
      tf.setMinCancellationFee(req.getMinCancellationFee());
    }
    if (req.getIdleFeePerMin() != null) {
      tf.setIdleFeePerMin(req.getIdleFeePerMin());
    }
    if (req.getTripDelayFeePerMin() != null) {
      tf.setTripDelayFeePerMin(req.getTripDelayFeePerMin());
    }
    if (req.getPenaltyFeeForCancel() != null) {
      tf.setPenaltyFeeForCancel(req.getPenaltyFeeForCancel());
    }
    if (req.getFeeAddToNext() != null) {
      tf.setFeeAddToNext(req.getFeeAddToNext());
    }

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
    // Strategy: Check ZoneV2 (zones table) first, then fall back to old Zone (zone table)
    // If found in old Zone table, we need to create/migrate it to ZoneV2 or return an error
    
    // 1. Try ZoneV2 by name (preferred method)
    if (req.getZoneName() != null && !req.getZoneName().isBlank()) {
      log.debug("Resolving zone by name in ZoneV2: {}", req.getZoneName());
      Optional<ZoneV2> zoneV2Opt = zoneV2Repository.findByNameAndIsActiveTrue(req.getZoneName());
      if (zoneV2Opt.isPresent()) {
        log.debug("Found zone in ZoneV2: id={}, name={}", zoneV2Opt.get().getId(), zoneV2Opt.get().getName());
        return zoneV2Opt.get();
      }
      log.debug("Zone not found in ZoneV2 by name: {}. Checking old Zone table...", req.getZoneName());
      
      // Fallback: Check old Zone table by name
      Optional<Zone> oldZoneOpt = zoneRepository.findAll().stream()
          .filter(z -> z.getName() != null && z.getName().equals(req.getZoneName()) && 
                      (z.getActive() != null && z.getActive()) && 
                      (z.getIsActive() != null && z.getIsActive()))
          .findFirst();
      
      if (oldZoneOpt.isPresent()) {
        Zone oldZone = oldZoneOpt.get();
        log.info("Zone '{}' found in old Zone table (id={}) but not in ZoneV2. Auto-creating in ZoneV2...", 
            req.getZoneName(), oldZone.getId());
        
        // Auto-create ZoneV2 from old Zone
        ZoneV2 newZone = ZoneV2.builder()
            .id(java.util.UUID.randomUUID().toString())
            .name(oldZone.getName())
            .coordinates(oldZone.getCoordinates() != null ? oldZone.getCoordinates() : oldZone.getPolygonWkt())
            .isActive(oldZone.getIsActive() != null ? oldZone.getIsActive() : 
                     (oldZone.getActive() != null ? oldZone.getActive() : true))
            .createdAt(oldZone.getCreatedAt() != null ? oldZone.getCreatedAt() : java.time.LocalDateTime.now())
            .updatedAt(java.time.LocalDateTime.now())
            .build();
        
        ZoneV2 savedZone = zoneV2Repository.save(newZone);
        log.info("Auto-created ZoneV2: id={}, name={}", savedZone.getId(), savedZone.getName());
        return savedZone;
      }
      
      log.warn("Active zone not found by name in either table: {}", req.getZoneName());
    }
    
    // 2. Try ZoneV2 by ID (UUID)
    if (req.getZoneId() != null && !req.getZoneId().isBlank()) {
      String zoneIdStr = req.getZoneId();
      // Check if it's a UUID (ZoneV2) or numeric (old Zone)
      boolean isUuid = zoneIdStr.length() == 36 && zoneIdStr.contains("-");
      
      if (isUuid) {
        log.debug("Resolving zone by UUID in ZoneV2: {}", zoneIdStr);
        Optional<ZoneV2> zoneV2Opt = zoneV2Repository.findById(zoneIdStr);
        if (zoneV2Opt.isPresent()) {
          log.debug("Found zone in ZoneV2 by UUID: id={}, name={}", zoneV2Opt.get().getId(), zoneV2Opt.get().getName());
          return zoneV2Opt.get();
        }
        log.warn("Zone not found in ZoneV2 by UUID: {}", zoneIdStr);
      } else {
        // Numeric ID - this is from old Zone table
        log.warn("Numeric zoneId detected: {}. ZoneV2 uses UUID strings. Trying to find by name instead.", zoneIdStr);
        // Don't try to look up numeric ID in ZoneV2, it won't work
      }
    }
    
    // 3. Error handling
    if (req.getZoneName() != null && !req.getZoneName().isBlank()) {
      throw new IllegalArgumentException("Active Zone not found by name: " + req.getZoneName() + 
          ". Please ensure the zone exists and is active in ZoneV2 (zones table). " +
          "If it exists in the old zone table, you need to create it in ZoneV2 first.");
    }
    if (req.getZoneId() != null && !req.getZoneId().isBlank()) {
      String zoneIdStr = req.getZoneId();
      boolean isUuid = zoneIdStr.length() == 36 && zoneIdStr.contains("-");
      if (!isUuid) {
        throw new IllegalArgumentException("Zone not found: " + req.getZoneId() + 
            ". ZoneV2 uses UUID string IDs, not numeric IDs. Please use zoneName instead.");
      } else {
        throw new IllegalArgumentException("Zone not found by UUID: " + req.getZoneId() + 
            ". Please ensure the zone exists in ZoneV2 (zones table).");
      }
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
