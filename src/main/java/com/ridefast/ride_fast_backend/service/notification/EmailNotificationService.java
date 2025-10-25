package com.ridefast.ride_fast_backend.service.notification;

public interface EmailNotificationService {
  void send(String to, String subject, String htmlBody);
}
