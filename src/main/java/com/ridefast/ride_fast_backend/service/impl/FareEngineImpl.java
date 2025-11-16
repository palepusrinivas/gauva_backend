package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.dto.FareEstimateRequest;
import com.ridefast.ride_fast_backend.dto.FareEstimateResponse;
import com.ridefast.ride_fast_backend.enums.ExtraFareStatus;
import com.ridefast.ride_fast_backend.enums.ServiceType;
import com.ridefast.ride_fast_backend.model.PricingProfile;
import com.ridefast.ride_fast_backend.model.ServiceRate;
import com.ridefast.ride_fast_backend.model.v2.ZoneV2;
import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.repository.PricingProfileRepository;
import com.ridefast.ride_fast_backend.repository.ServiceRateRepository;
import com.ridefast.ride_fast_backend.repository.v2.ZoneV2Repository;
import com.ridefast.ride_fast_backend.repository.v2.VehicleCategoryRepository;
import com.ridefast.ride_fast_backend.repository.v2.TripFareRepository;
import com.ridefast.ride_fast_backend.service.FareEngine;
import com.ridefast.ride_fast_backend.service.promo.CouponService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FareEngineImpl implements FareEngine {

  private final PricingProfileRepository pricingProfileRepository;
  private final ServiceRateRepository serviceRateRepository;
  private final ZoneV2Repository zoneRepository;
  private final VehicleCategoryRepository vehicleCategoryRepository;
  private final TripFareRepository tripFareRepository;
  private final CouponService couponService;

  @Override
  public FareEstimateResponse estimate(FareEstimateRequest req) {
    PricingProfile profile = pricingProfileRepository.findFirstByActiveTrue()
        .orElseThrow(() -> new IllegalStateException("No active pricing profile configured"));

    ServiceType type = req.getServiceType() != null ? req.getServiceType() : ServiceType.MEGA;
    Optional<ServiceRate> rateOpt = serviceRateRepository.findByPricingProfileAndServiceType(profile, type);

    double baseFare = rateOpt.map(ServiceRate::getBaseFare).orElse(profile.getBaseFare());
    double perKmRate = rateOpt.map(ServiceRate::getPerKmRate).orElse(profile.getPerKmRate());
    double timeRatePerMin = rateOpt.map(ServiceRate::getTimeRatePerMin)
        .filter(v -> v > 0)
        .orElse(profile.getTimeRatePerMin());

    // Zone overrides (admin-managed). Pickup zone takes precedence.
    ExtraFareStatus extraStatus = ExtraFareStatus.NONE;
    String extraReason = null;

    ZoneV2 pickupZone = null;
    if (req.getPickupZoneReadableId() != null) {
      pickupZone = zoneRepository.findByNameAndIsActiveTrue(req.getPickupZoneReadableId()).orElse(null);
      if (pickupZone != null) {
        applyZoneOverride(profile, pickupZone);
      }
    }
    ZoneV2 dropZone = null;
    if (extraStatus == ExtraFareStatus.NONE && req.getDropZoneReadableId() != null) {
      dropZone = zoneRepository.findByNameAndIsActiveTrue(req.getDropZoneReadableId()).orElse(null);
    }

    // Apply trip_fares overrides if available (zone + category)
    ZoneV2 zoneForOverride = pickupZone != null ? pickupZone : dropZone;
    if (zoneForOverride != null) {
      String categoryType = type.name();
      Optional<VehicleCategory> catOpt = vehicleCategoryRepository.findByType(categoryType);
      if (catOpt.isPresent()) {
        Optional<TripFare> tfOpt = tripFareRepository.findFirstByZoneAndVehicleCategory(zoneForOverride, catOpt.get());
        if (tfOpt.isPresent()) {
          TripFare tf = tfOpt.get();
          if (tf.getBaseFare() != null) baseFare = tf.getBaseFare().doubleValue();
          if (tf.getBaseFarePerKm() != null) perKmRate = tf.getBaseFarePerKm().doubleValue();
          if (tf.getTimeRatePerMinOverride() != null) timeRatePerMin = tf.getTimeRatePerMinOverride().doubleValue();
        }
      }
    }

    double distanceFare = round2(req.getDistanceKm() * perKmRate);
    double timeFare = round2(req.getDurationMin() * timeRatePerMin);

    double cancellationFee = 0.0; // estimates ignore unless you want flags
    double returnFee = 0.0; // estimates ignore unless you want flags

    double total = round2(baseFare + distanceFare + timeFare + cancellationFee + returnFee);

    double discount = 0.0;
    String appliedCoupon = null;
    if (req.getCouponCode() != null && !req.getCouponCode().isBlank() && req.getUserId() != null) {
      try {
        discount = couponService
            .computeDiscount(req.getCouponCode(), req.getUserId(), java.math.BigDecimal.valueOf(total))
            .doubleValue();
        appliedCoupon = discount > 0 ? req.getCouponCode() : null;
      } catch (Exception ignored) {
        // invalid coupon -> treat as no discount
      }
    }
    double finalTotal = round2(Math.max(0.0, total - discount));

    return FareEstimateResponse.builder()
        .currency(profile.getCurrency())
        .baseFare(baseFare)
        .distanceKm(req.getDistanceKm())
        .perKmRate(perKmRate)
        .distanceFare(distanceFare)
        .durationMin(req.getDurationMin())
        .timeRatePerMin(timeRatePerMin)
        .timeFare(timeFare)
        .cancellationFee(cancellationFee)
        .returnFee(returnFee)
        .total(total)
        .discount(discount)
        .finalTotal(finalTotal)
        .appliedCoupon(appliedCoupon)
        .extraFareStatus(extraStatus)
        .extraFareReason(extraReason)
        .build();
  }

  private static double round2(double v) {
    return Math.round(v * 100.0) / 100.0;
  }

  // placeholder for future side-effects when applying zone (e.g., logs)
  private void applyZoneOverride(PricingProfile profile, ZoneV2 zone) {
  }
}
