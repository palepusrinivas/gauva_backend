package com.ridefast.ride_fast_backend.service.storage.impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FirebaseStorageServiceImpl implements StorageService {
  private static final Logger log = LoggerFactory.getLogger(FirebaseStorageServiceImpl.class);

  @Value("${app.firebase.limits.doc-max-bytes:2097152}")
  private long docMaxBytes; // default 2MB

  @Value("${app.firebase.limits.log-max-bytes:5242880}")
  private long logMaxBytes; // default 5MB

  @Value("${app.firebase.storage-bucket:}")
  private String defaultBucket;

  @Value("${app.firebase.documents-gs-path:gs://gauva-15d9a.appspot.com/documents}")
  private String documentsGsPath;

  @Value("${app.firebase.logs-gs-path:gs://gauva-15d9a.appspot.com/app_logs}")
  private String logsGsPath;

  private record GsInfo(String bucket, String prefix) {}

  private GsInfo parseGs(String gsPath) {
    // Expect format: gs://bucket/prefix
    if (gsPath == null || !gsPath.startsWith("gs://")) {
      return new GsInfo(defaultBucket, "");
    }
    String rest = gsPath.substring(5); // after gs://
    int slash = rest.indexOf('/');
    if (slash < 0) {
      return new GsInfo(rest, "");
    }
    String bucket = rest.substring(0, slash);
    String prefix = rest.substring(slash + 1);
    if (!prefix.isEmpty() && !prefix.endsWith("/")) {
      prefix = prefix + "/";
    }
    return new GsInfo(bucket, prefix);
  }

  private String put(byte[] data, String contentType, String objectName, String baseGsPath, long maxBytes) {
    if (data == null) throw new IllegalArgumentException("data is null");
    if (data.length > maxBytes) {
      throw new IllegalArgumentException("File too large");
    }
    if (objectName == null || objectName.isBlank()) {
      throw new IllegalArgumentException("objectName is required");
    }

    GsInfo info = parseGs(baseGsPath);
    String bucketName = (info.bucket() != null && !info.bucket().isBlank()) ? info.bucket() : defaultBucket;
    String objectPath = info.prefix() + objectName;

    log.info("Firebase upload attempt: bucket={}, objectPath={}, contentType={}, size={}", 
        bucketName, objectPath, contentType, data.length);

    try {
      if (bucketName == null || bucketName.isBlank()) {
        log.error("Firebase bucket name is not configured. Set app.firebase.storage-bucket or provide a valid gs:// path in config.");
        throw new RuntimeException("Upload failed: bucket not configured");
      }

      Bucket bucket = null;
      Exception bucketException = null;
      try {
        log.info("Attempting to access Firebase bucket: {}", bucketName);
        bucket = StorageClient.getInstance().bucket(bucketName);
        log.info("Successfully accessed bucket: {}", bucketName);
      } catch (Exception be) {
        bucketException = be;
        log.warn("Configured bucket '{}' not accessible: {}. Attempting default FirebaseApp bucket...", bucketName, be.getMessage());
      }
      
      if (bucket == null) {
        try {
          bucket = StorageClient.getInstance().bucket(); // default app bucket
          if (bucket != null) {
            bucketName = bucket.getName();
            log.warn("Falling back to default Firebase bucket: {}", bucketName);
          }
        } catch (Exception de) {
          log.error("Default bucket also not accessible: {}", de.getMessage());
        }
      }
      
      if (bucket == null) {
        String errorMsg = "No Firebase Storage bucket available. ";
        if (bucketException != null) {
          errorMsg += "Original error: " + bucketException.getMessage();
        }
        log.error(errorMsg + " Verify that: 1) Firebase Storage is enabled in Firebase Console, 2) Service account has Storage Admin role, 3) 'app.firebase.storage-bucket' is correct (e.g., gauva-15d9a.appspot.com)");
        throw new RuntimeException("Upload failed: " + errorMsg);
      }

      log.info("Creating blob in bucket {} at path {}", bucket.getName(), objectPath);
      Blob blob = bucket.create(objectPath, data, contentType);
      String gs = String.format("gs://%s/%s", bucket.getName(), objectPath);
      log.info("Successfully uploaded to Firebase Storage: {}", gs);
      return gs;
    } catch (RuntimeException re) {
      throw re; // Re-throw our own exceptions
    } catch (Exception e) {
      log.error("Failed to upload to Firebase Storage [bucket={}, object={}]: {}", bucketName, objectPath, e.toString(), e);
      throw new RuntimeException("Upload failed: " + e.getMessage());
    }
  }

  @Override
  public String uploadDriverDocument(byte[] data, String contentType, String objectName) {
    return put(data, contentType, objectName, documentsGsPath, docMaxBytes);
  }

  @Override
  public String uploadAppLog(byte[] data, String contentType, String objectName) {
    return put(data, contentType, objectName, logsGsPath, logMaxBytes);
  }

  @Override
  public String uploadFile(MultipartFile file, String objectName) {
    try {
      if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException("File is empty");
      }
      byte[] data = file.getBytes();
      String contentType = file.getContentType();
      // Use documents path for general file uploads, with 5MB limit
      String gsPath = put(data, contentType, objectName, documentsGsPath, 5 * 1024 * 1024);
      
      // Extract the storage key/path from gs:// path
      // gsPath format: gs://bucket/path/to/file
      // We need to return just the path part (without gs://bucket/) so getPublicUrl can construct the URL
      if (gsPath == null || !gsPath.startsWith("gs://")) {
        throw new RuntimeException("Invalid gsPath returned from put(): " + gsPath);
      }
      
      // Extract path after gs://bucket/
      String rest = gsPath.substring(5); // after "gs://"
      int slashIndex = rest.indexOf('/');
      if (slashIndex < 0) {
        throw new RuntimeException("Invalid gsPath format: " + gsPath);
      }
      
      // Get the object path (everything after bucket name)
      String objectPath = rest.substring(slashIndex + 1);
      
      // Remove trailing slash if present
      if (objectPath.endsWith("/")) {
        objectPath = objectPath.substring(0, objectPath.length() - 1);
      }
      
      log.info("Uploaded file successfully. Storage key: {}", objectPath);
      
      // Return the storage key (path) instead of full URL
      // This allows getPublicUrl() to construct the URL properly
      return objectPath;
    } catch (Exception e) {
      log.error("Failed to upload file: {}", e.getMessage(), e);
      throw new RuntimeException("File upload failed: " + e.getMessage());
    }
  }

  @Override
  public boolean delete(String objectGsPath) {
    try {
      GsInfo info = parseGs(objectGsPath);
      String bucketName = (info.bucket() != null && !info.bucket().isBlank()) ? info.bucket() : defaultBucket;
      if (bucketName == null || bucketName.isBlank()) return false;
      String objectPath = info.prefix();
      if (objectPath.endsWith("/")) return false; // path points to prefix only
      Bucket bucket = StorageClient.getInstance().bucket(bucketName);
      Blob existing = bucket.get(objectPath);
      return existing != null && existing.delete();
    } catch (Exception e) {
      log.error("Failed to delete from Firebase Storage: {}", e.toString());
      return false;
    }
  }
}
