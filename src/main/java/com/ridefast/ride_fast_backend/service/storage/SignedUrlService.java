package com.ridefast.ride_fast_backend.service.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.SignUrlOption;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SignedUrlService {
  @Value("${app.firebase.storage-bucket:}")
  private String defaultBucket;

  @Value("${app.firebase.credentials-b64:}")
  private String credentialsB64;

  @Value("${app.firebase.credentials-path:}")
  private String credentialsPath;

  @Value("${app.firebase.documents-gs-path:}")
  private String documentsGsPath;

  @Value("${app.firebase.logs-gs-path:}")
  private String logsGsPath;

  private record GsInfo(String bucket, String prefix) {}

  private GsInfo parseGs(String gsPath) {
    if (gsPath == null || !gsPath.startsWith("gs://")) {
      return new GsInfo(defaultBucket, "");
    }
    String rest = gsPath.substring(5);
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

  private ServiceAccountCredentials loadServiceAccount() throws Exception {
    GoogleCredentials gc;
    if (credentialsB64 != null && !credentialsB64.isBlank()) {
      byte[] decoded = Base64.getDecoder().decode(credentialsB64);
      try (InputStream is = new ByteArrayInputStream(decoded)) {
        gc = GoogleCredentials.fromStream(is);
      }
    } else if (credentialsPath != null && !credentialsPath.isBlank()) {
      try (InputStream is = new FileInputStream(credentialsPath)) {
        gc = GoogleCredentials.fromStream(is);
      }
    } else {
      gc = GoogleCredentials.getApplicationDefault();
    }
    if (!(gc instanceof ServiceAccountCredentials sac)) {
      throw new IllegalStateException("Service account credentials required for signing URLs");
    }
    return sac;
  }

  private URL sign(String baseGsPath, String objectName, int minutes) {
    if (objectName == null || objectName.isBlank()) throw new IllegalArgumentException("objectName required");
    try {
      GsInfo info = parseGs(baseGsPath);
      String bucket = (info.bucket() != null && !info.bucket().isBlank()) ? info.bucket() : defaultBucket;
      String objectPath = info.prefix() + objectName;
      ServiceAccountCredentials sac = loadServiceAccount();
      Storage storage = StorageOptions.newBuilder().setCredentials(sac).build().getService();
      BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectPath).build();
      return storage.signUrl(
          blobInfo,
          minutes,
          TimeUnit.MINUTES,
          SignUrlOption.signWith(sac),
          SignUrlOption.withV4Signature());
    } catch (Exception e) {
      throw new RuntimeException("Failed to sign URL");
    }
  }

  public URL signDocument(String objectName, int minutes) {
    return sign(documentsGsPath, objectName, minutes);
  }

  public URL signLog(String objectName, int minutes) {
    return sign(logsGsPath, objectName, minutes);
  }
}
