package com.ridefast.ride_fast_backend.service.logs;

public interface LogUploadService {
  String uploadLatest();
  String uploadByName(String fileName);
}
