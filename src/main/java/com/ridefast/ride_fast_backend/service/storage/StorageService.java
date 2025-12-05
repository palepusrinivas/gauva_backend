package com.ridefast.ride_fast_backend.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
  String uploadDriverDocument(byte[] data, String contentType, String objectName);
  String uploadAppLog(byte[] data, String contentType, String objectName);
  String uploadFile(MultipartFile file, String objectName);
  boolean delete(String objectPath);
}
