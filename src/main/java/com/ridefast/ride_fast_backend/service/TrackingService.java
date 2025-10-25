package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.dto.LocationUpdate;

public interface TrackingService {
  void saveLastLocation(Long rideId, LocationUpdate update);
  LocationUpdate getLastLocation(Long rideId);
}
