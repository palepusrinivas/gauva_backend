package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.model.PricingConfig;
import com.ridefast.ride_fast_backend.dto.PricingConfigRequest;

public interface PricingConfigService {
  PricingConfig getOrCreate();
  PricingConfig update(PricingConfigRequest request);
}
