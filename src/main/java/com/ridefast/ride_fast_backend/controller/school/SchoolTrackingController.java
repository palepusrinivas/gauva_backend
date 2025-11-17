package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.SchoolDriver;
import com.ridefast.ride_fast_backend.model.school.TrackingPing;
import com.ridefast.ride_fast_backend.repository.school.BusRepository;
import com.ridefast.ride_fast_backend.repository.school.SchoolDriverRepository;
import com.ridefast.ride_fast_backend.repository.school.TrackingPingRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchoolTrackingController {

	private final BusRepository busRepository;
	private final SchoolDriverRepository schoolDriverRepository;
	private final TrackingPingRepository trackingPingRepository;

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

	@PostMapping("/tracking")
	public ResponseEntity<?> updateBusLocation(@RequestBody TrackingUpdateRequest req) {
		if (req.getBusId() == null || req.getLat() == null || req.getLng() == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "bus_id, lat, and lng are required"));
		}

		Bus bus = busRepository.findById(req.getBusId()).orElse(null);
		if (bus == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Bus not found"));
		}

		SchoolDriver driver = null;
		if (req.getDriverId() != null) {
			driver = schoolDriverRepository.findById(req.getDriverId()).orElse(null);
		}

		TrackingPing ping = new TrackingPing();
		ping.setBus(bus);
		ping.setDriver(driver);
		ping.setLatitude(req.getLat());
		ping.setLongitude(req.getLng());
		ping.setSpeed(req.getSpeed());
		ping.setHeading(req.getHeading());
		ping.setCreatedAt(req.getTimestamp() != null ? req.getTimestamp() : LocalDateTime.now());

		TrackingPing saved = trackingPingRepository.save(ping);

		return ResponseEntity.ok(Map.of(
			"status", "ok",
			"trackingId", saved.getId(),
			"busId", bus.getId(),
			"timestamp", saved.getCreatedAt()
		));
	}

	@Data
	public static class TrackingUpdateRequest {
		private Long busId;
		private Long driverId;
		private Double lat;
		private Double lng;
		private Float speed;
		private Float heading;
		private LocalDateTime timestamp;
	}
}


