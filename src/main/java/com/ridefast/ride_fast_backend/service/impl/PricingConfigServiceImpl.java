package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.dto.PricingConfigRequest;
import com.ridefast.ride_fast_backend.model.PricingConfig;
import com.ridefast.ride_fast_backend.repository.PricingConfigRepository;
import com.ridefast.ride_fast_backend.service.PricingConfigService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingConfigServiceImpl implements PricingConfigService {

  private final PricingConfigRepository repo;

  @Override
  @Transactional
  public PricingConfig getOrCreate() {
    Optional<PricingConfig> existing = repo.findAll().stream().findFirst();
    if (existing.isPresent()) return existing.get();
    PricingConfig cfg = PricingConfig.builder()
        // Auto defaults
        .autoBaseFare(50)
        .autoPerKmFare(15)
        .autoNightSurchargePercent(15)
        .autoPlatformFeeFlat(7)
        // Bike defaults
        .bikeCommissionPercent(7)
        .bikeNightSurchargePercent(15)
        // Car defaults
        .carCommissionPercent(7)
        .carNightSurchargePercent(15)
        // Common
        .gstPercent(5)
        .firstRideCashbackPercent(20)
        .walletUsageCapPercent(10)
        .walletCreditValidityDays(7)
        // Night window
        .nightStartHour(22)
        .nightEndHour(6)
        // Payouts
        .minPayoutAmount(0)
        .payoutFeeFlat(0)
        .payoutFeePercent(0)
        .build();
    return repo.save(cfg);
  }

  @Override
  @Transactional
  public PricingConfig update(PricingConfigRequest r) {
    PricingConfig cfg = getOrCreate();
    // Auto
    if (r.getAutoBaseFare() != null) cfg.setAutoBaseFare(r.getAutoBaseFare());
    if (r.getAutoPerKmFare() != null) cfg.setAutoPerKmFare(r.getAutoPerKmFare());
    if (r.getAutoNightSurchargePercent() != null) cfg.setAutoNightSurchargePercent(r.getAutoNightSurchargePercent());
    if (r.getAutoPlatformFeeFlat() != null) cfg.setAutoPlatformFeeFlat(r.getAutoPlatformFeeFlat());
    // Bike
    if (r.getBikeCommissionPercent() != null) cfg.setBikeCommissionPercent(r.getBikeCommissionPercent());
    if (r.getBikeNightSurchargePercent() != null) cfg.setBikeNightSurchargePercent(r.getBikeNightSurchargePercent());
    // Car
    if (r.getCarCommissionPercent() != null) cfg.setCarCommissionPercent(r.getCarCommissionPercent());
    if (r.getCarNightSurchargePercent() != null) cfg.setCarNightSurchargePercent(r.getCarNightSurchargePercent());
    // Common
    if (r.getGstPercent() != null) cfg.setGstPercent(r.getGstPercent());
    if (r.getFirstRideCashbackPercent() != null) cfg.setFirstRideCashbackPercent(r.getFirstRideCashbackPercent());
    if (r.getWalletUsageCapPercent() != null) cfg.setWalletUsageCapPercent(r.getWalletUsageCapPercent());
    if (r.getWalletCreditValidityDays() != null) cfg.setWalletCreditValidityDays(r.getWalletCreditValidityDays());
    // Night window
    if (r.getNightStartHour() != null) cfg.setNightStartHour(r.getNightStartHour());
    if (r.getNightEndHour() != null) cfg.setNightEndHour(r.getNightEndHour());
    // Payouts
    if (r.getMinPayoutAmount() != null) cfg.setMinPayoutAmount(r.getMinPayoutAmount());
    if (r.getPayoutFeeFlat() != null) cfg.setPayoutFeeFlat(r.getPayoutFeeFlat());
    if (r.getPayoutFeePercent() != null) cfg.setPayoutFeePercent(r.getPayoutFeePercent());

    return repo.save(cfg);
  }
}
