package com.ridefast.ride_fast_backend.service.storage.impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirebaseStorageServiceImpl implements StorageService {
  private static final Logger log = LoggerFactory.getLogger(FirebaseStorageServiceImpl.class);

  @Value("${app.firebase.limits.doc-max-bytes:2097152}")
  private long docMaxBytes; // default 2MB

  @Value("${app.firebase.limits.log-max-bytes:5242880}")
  private long logMaxBytes; // default 5MB

  @Value("${app.firebase.storage-bucket:}")
  private String defaultBucket;

  @Value("${app.firebase.documents-gs-path:gs://gauva-15d9a.firebasestorage.app/documents}")
  private String documentsGsPath;

  @Value("${app.firebase.logs-gs-path:gs://gauva-15d9a.firebasestorage.app/app_logs}")
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

    try {
      if (bucketName == null || bucketName.isBlank()) {
        log.error("Firebase bucket name is not configured. Set app.firebase.storage-bucket or provide a valid gs:// path in config.");
        throw new RuntimeException("Upload failed");
      }

      Bucket bucket = null;
      try {
        bucket = StorageClient.getInstance().bucket(bucketName);
      } catch (Exception be) {
        log.warn("Configured bucket '{}' not accessible. Attempting default FirebaseApp bucket...", bucketName);
      }
      if (bucket == null) {
        try {
          bucket = StorageClient.getInstance().bucket(); // default app bucket
          if (bucket != null) {
            bucketName = bucket.getName();
            log.warn("Falling back to default Firebase bucket: {}", bucketName);
          }
        } catch (Exception de) {
          // ignore; handled below
        }
      }
      if (bucket == null) {
        log.error("No Firebase Storage bucket available. Verify that Firebase Storage is enabled and 'app.firebase.storage-bucket' is correct (e.g., <project-id>.appspot.com).");
        throw new RuntimeException("Upload failed");
      }

      Blob blob = bucket.create(objectPath, data, contentType);
      String gs = String.format("gs://%s/%s", bucket.getName(), objectPath);
      return gs;
    } catch (Exception e) {
      log.error("Failed to upload to Firebase Storage [bucket={}, object={}]: {}", bucketName, objectPath, e.toString());
      throw new RuntimeException("Upload failed");
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
