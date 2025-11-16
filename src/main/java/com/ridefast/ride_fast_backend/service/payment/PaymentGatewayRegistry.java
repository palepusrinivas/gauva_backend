package com.ridefast.ride_fast_backend.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRegistry {

    private final Map<String, PaymentGateway> byName = new ConcurrentHashMap<>();

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        gateways.forEach(g -> byName.put(g.getName().toLowerCase(), g));
    }

    public PaymentGateway get(String name) {
        return byName.get(name.toLowerCase());
    }
}


