package com.ridefast.ride_fast_backend.service.payment;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {
    String getName();
    Map<String, Object> createPayment(String orderId, BigDecimal amount, String currency, String callbackUrl);
    boolean handleWebhook(Map<String, Object> payload);
}


