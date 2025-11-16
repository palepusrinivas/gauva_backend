package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.SubscriptionParent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class MockPaymentProvider implements PaymentProvider {
	@Override
	public String createPaymentLink(SubscriptionParent subscription) {
		return "https://pay.example.com/mock/" + subscription.getId();
	}

	@Override
	public void handleWebhook(java.util.Map<String, Object> payload) throws IllegalArgumentException {
		// Accept all in mock
	}
}


