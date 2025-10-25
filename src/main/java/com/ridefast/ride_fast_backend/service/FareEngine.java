package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.dto.FareEstimateRequest;
import com.ridefast.ride_fast_backend.dto.FareEstimateResponse;

public interface FareEngine {
  FareEstimateResponse estimate(FareEstimateRequest req);
}
