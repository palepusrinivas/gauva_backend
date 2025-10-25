package com.ridefast.ride_fast_backend.service.notification;

public interface SmsNotificationService {
  void send(String phoneNumber, String message);
}
