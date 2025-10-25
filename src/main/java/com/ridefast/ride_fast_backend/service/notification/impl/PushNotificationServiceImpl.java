package com.ridefast.ride_fast_backend.service.notification.impl;

import com.ridefast.ride_fast_backend.service.notification.PushNotificationService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

  private static final Logger log = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

  @Override
  public void sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
    try {
      Map<String, String> safeData = (data != null) ? data : new HashMap<>();

      Notification notification = Notification.builder()
          .setTitle(title)
          .setBody(body)
          .build();

      AndroidConfig androidConfig = AndroidConfig.builder()
          .setPriority(AndroidConfig.Priority.HIGH)
          .build();

      Message message = Message.builder()
          .setToken(fcmToken)
          .setNotification(notification)
          .putAllData(safeData)
          .setAndroidConfig(androidConfig)
          .build();

      String messageId = FirebaseMessaging.getInstance().send(message);
      log.debug("FCM sent messageId={} to token={}", messageId, fcmToken);
    } catch (Exception e) {
      log.error("Error sending FCM push: {}", e.getMessage());
    }
  }
}
