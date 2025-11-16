package com.ridefast.ride_fast_backend.controller.school;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SchoolTrackingController {

	@GetMapping("/buses/{busId}/location")
	public ResponseEntity<?> getBusLocation(@PathVariable Long busId) {
		// Stub: integrate with Firebase later
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
				"message", "Realtime location via Firebase not implemented",
				"busId", busId
		));
	}

	@PostMapping("/drivers/{driverId}/start_trip")
	public ResponseEntity<?> startTrip(@PathVariable Long driverId, @RequestBody(required = false) Map<String, Object> body) {
		// Stub: set Firebase trip state here
		return ResponseEntity.accepted().body(Map.of(
				"status", "trip_start_received",
				"driverId", driverId,
				"payload", body
		));
	}

	@PostMapping("/drivers/{driverId}/mark_stop_reached")
	public ResponseEntity<?> markStopReached(@PathVariable Long driverId, @RequestBody Map<String, Object> body) {
		// Stub: trigger alerts to parents mapped to that stop
		return ResponseEntity.accepted().body(Map.of(
				"status", "stop_reached_received",
				"driverId", driverId,
				"payload", body
		));
	}
}


