package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.BusActivation;
import com.ridefast.ride_fast_backend.model.school.Student;
import com.ridefast.ride_fast_backend.model.school.SubscriptionParent;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.school.*;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.BusActivation;
import com.ridefast.ride_fast_backend.model.school.Student;
import com.ridefast.ride_fast_backend.model.school.SubscriptionParent;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.school.*;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchoolPaymentController {

	private final UserRepository userRepository;
	private final StudentRepository studentRepository;
	private final SubscriptionParentRepository subscriptionRepository;
	private final BusRepository busRepository;
	private final BusActivationRepository busActivationRepository;

	@PostMapping("/parents/{userId}/subscriptions")
	public ResponseEntity<Map<String, String>> createSubscription(@PathVariable String userId, @RequestBody @Valid CreateSubscriptionRequest req) {
		MyUser user = userRepository.findById(userId).orElse(null);
		Student student = studentRepository.findById(req.getStudentId()).orElse(null);
		if (user == null || student == null) return ResponseEntity.notFound().build();
		SubscriptionParent sub = new SubscriptionParent();
		sub.setUser(user);
		sub.setStudent(student);
		sub.setPlanType(req.getPlanType());
		sub.setPrice(req.getPrice());
		sub.setStatus("pending");
		subscriptionRepository.save(sub);
		// return mock payment link for now
		return new ResponseEntity<>(Map.of("paymentLink", "https://pay.example.com/mock/" + sub.getId()), HttpStatus.CREATED);
	}

	@PostMapping("/payments/webhook")
	public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
		// MOCK: mark subscription active based on provided subscriptionId
		Object subIdObj = payload.get("subscriptionId");
		if (subIdObj == null) return ResponseEntity.badRequest().body("subscriptionId required");
		Long subId = Long.valueOf(subIdObj.toString());
		SubscriptionParent sub = subscriptionRepository.findById(subId).orElse(null);
		if (sub == null) return ResponseEntity.notFound().build();
		sub.setStatus("active");
		sub.setStartDate(LocalDate.now());
		sub.setEndDate(LocalDate.now().plusMonths(1));
		subscriptionRepository.save(sub);
		return ResponseEntity.ok(Map.of("status", "ok"));
	}

	@GetMapping("/parents/{userId}/subscriptions")
	public ResponseEntity<List<SubscriptionParent>> listSubscriptions(@PathVariable String userId) {
		MyUser user = userRepository.findById(userId).orElse(null);
		if (user == null) return ResponseEntity.notFound().build();
		return ResponseEntity.ok(subscriptionRepository.findByUser(user));
	}

	@PostMapping("/buses/{id}/activate")
	public ResponseEntity<BusActivation> activateBus(@PathVariable Long id, @RequestBody(required = false) ActivateBusRequest req) {
		Bus bus = busRepository.findById(id).orElse(null);
		if (bus == null) return ResponseEntity.notFound().build();
		BusActivation act = new BusActivation();
		act.setBus(bus);
		act.setActivationFee(req != null && req.getActivationFee() != null ? req.getActivationFee() : 354);
		act.setStartDate(LocalDate.now());
		act.setEndDate(LocalDate.now().plusMonths(1));
		act.setStatus("active");
		return new ResponseEntity<>(busActivationRepository.save(act), HttpStatus.CREATED);
	}

	@Data
	public static class CreateSubscriptionRequest {
		private Long studentId;
		private String planType;
		private Integer price;
	}

	@Data
	public static class ActivateBusRequest {
		private Integer activationFee;
	}
}


