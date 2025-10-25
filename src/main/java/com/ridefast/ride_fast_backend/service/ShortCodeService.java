package com.ridefast.ride_fast_backend.service;

public interface ShortCodeService {
  String generateUserCode();
  String generateDriverCode();
  String generateRideCode();
}
