package com.ridefast.ride_fast_backend.service.notification.impl;

import com.ridefast.ride_fast_backend.service.notification.SmsNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsNotificationServiceImpl implements SmsNotificationService {

  private static final Logger log = LoggerFactory.getLogger(SmsNotificationServiceImpl.class);

  @Value("${app.notify.sms.provider:dummy}")
  private String provider;

  @Value("${app.notify.sms.api-key:}")
  private String apiKey;

  @Value("${app.notify.sms.sender-id:RIDFST}")
  private String senderId;

  @Override
  public void send(String phoneNumber, String message) {
    // Dummy implementation: just log. Replace with actual provider integration.
    log.info("[SMS:{}] to={} sender={} msg={}", provider, phoneNumber, senderId, message);
  }
}
