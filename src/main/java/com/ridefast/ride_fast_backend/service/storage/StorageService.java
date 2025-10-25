package com.ridefast.ride_fast_backend.service.storage;

public interface StorageService {
  String uploadDriverDocument(byte[] data, String contentType, String objectName);
  String uploadAppLog(byte[] data, String contentType, String objectName);
  boolean delete(String objectPath);
}
