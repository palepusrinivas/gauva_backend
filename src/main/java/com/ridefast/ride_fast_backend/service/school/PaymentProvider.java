package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.SubscriptionParent;

public interface PaymentProvider {
	String createPaymentLink(SubscriptionParent subscription);
	void handleWebhook(java.util.Map<String, Object> payload) throws IllegalArgumentException;
}


