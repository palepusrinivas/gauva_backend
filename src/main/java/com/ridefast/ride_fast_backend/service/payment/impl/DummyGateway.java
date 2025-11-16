package com.ridefast.ride_fast_backend.service.payment.impl;

import com.ridefast.ride_fast_backend.service.payment.PaymentGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class DummyGateway implements PaymentGateway {
    @Override
    public String getName() {
        return "DUMMY";
    }

    @Override
    public Map<String, Object> createPayment(String orderId, BigDecimal amount, String currency, String callbackUrl) {
        return Map.of(
                "provider", getName(),
                "orderId", orderId,
                "amount", amount,
                "currency", currency,
                "paymentUrl", callbackUrl + "?orderId=" + orderId + "&status=success"
        );
    }

    @Override
    public boolean handleWebhook(Map<String, Object> payload) {
        return true;
    }
}


