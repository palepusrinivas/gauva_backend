package com.ridefast.ride_fast_backend.service.notification;

import java.util.Map;

public interface PushNotificationService {
  void sendToToken(String fcmToken, String title, String body, Map<String, String> data);
}
