package com.ridefast.ride_fast_backend.service.notification.impl;

import com.ridefast.ride_fast_backend.service.notification.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

  private final JavaMailSender mailSender;

  @Value("${app.notify.email.from:no-reply@example.com}")
  private String from;

  @Override
  public void send(String to, String subject, String htmlBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(message);
    } catch (Exception ex) {
      // swallow to prevent crashing flows; in production add retry/queue
      org.slf4j.LoggerFactory.getLogger(EmailNotificationServiceImpl.class)
          .error("Failed to send email to {}: {}", to, ex.getMessage());
    }
  }
}
