package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.school.*;
import com.ridefast.ride_fast_backend.repository.school.*;
import com.ridefast.ride_fast_backend.service.school.BusActivationService;
import com.ridefast.ride_fast_backend.service.school.ParentRequestService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/school")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchoolController {

	private final InstitutionRepository institutionRepository;
	private final BranchRepository branchRepository;
	private final BusRepository busRepository;
	private final StudentRepository studentRepository;
	private final ParentRequestRepository parentRequestRepository;
	private final BusActivationRepository busActivationRepository;
	private final TrackingPingRepository trackingPingRepository;
	private final AlertLogRepository alertLogRepository;
	private final SchoolDriverRepository schoolDriverRepository;
	private final StopRepository stopRepository;
	private final BusActivationService busActivationService;
	private final ParentRequestService parentRequestService;

	// ========== INSTITUTIONS ==========
	@GetMapping("/institutions")
	public ResponseEntity<List<Institution>> getAllInstitutions() {
		return ResponseEntity.ok(institutionRepository.findAll());
	}

	@GetMapping("/institutions/{id}")
	public ResponseEntity<Institution> getInstitution(@PathVariable Long id) {
		return institutionRepository.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// ========== BRANCHES ==========
	@GetMapping("/branches")
	public ResponseEntity<List<Branch>> getAllBranches(
		@RequestParam(required = false) Long institutionId
	) {
		if (institutionId != null) {
			Institution institution = institutionRepository.findById(institutionId).orElse(null);
			if (institution == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(branchRepository.findByInstitution(institution));
		}
		return ResponseEntity.ok(branchRepository.findAll());
	}

	@GetMapping("/branches/{id}")
	public ResponseEntity<Branch> getBranch(@PathVariable Long id) {
		return branchRepository.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// ========== BUSES ==========
	@GetMapping("/buses")
	public ResponseEntity<List<Bus>> getAllBuses(
		@RequestParam(required = false) Long branchId
	) {
		if (branchId != null) {
			Branch branch = branchRepository.findById(branchId).orElse(null);
			if (branch == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(busRepository.findByBranch(branch));
		}
		return ResponseEntity.ok(busRepository.findAll());
	}

	@GetMapping("/buses/{id}")
	public ResponseEntity<Bus> getBus(@PathVariable Long id) {
		return busRepository.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/buses/{id}/activation")
	public ResponseEntity<?> getBusActivation(@PathVariable Long id) {
		Bus bus = busRepository.findById(id).orElse(null);
		if (bus == null) {
			return ResponseEntity.notFound().build();
		}
		Optional<BusActivation> activation = busActivationService.findActiveByBus(bus);
		if (activation.isPresent()) {
			return ResponseEntity.ok(activation.get());
		}
		return ResponseEntity.ok(Map.of("message", "No active activation found"));
	}

	@PostMapping("/buses/{id}/activate")
	public ResponseEntity<?> activateBus(@PathVariable Long id) {
		Bus bus = busRepository.findById(id).orElse(null);
		if (bus == null) {
			return ResponseEntity.notFound().build();
		}
		BusActivation activation = busActivationService.activateBus(bus);
		return ResponseEntity.ok(activation);
	}

	// ========== PARENT REQUESTS ==========
	@GetMapping("/parent-requests")
	public ResponseEntity<List<ParentRequest>> getAllParentRequests(
		@RequestParam(required = false) String status,
		@RequestParam(required = false) Long branchId
	) {
		List<ParentRequest> requests;
		if (status != null) {
			requests = parentRequestService.findByStatus(status);
		} else {
			requests = parentRequestService.findAll();
		}

		if (branchId != null) {
			Branch branch = branchRepository.findById(branchId).orElse(null);
			if (branch != null) {
				requests = requests.stream()
					.filter(r -> r.getBranch() != null && r.getBranch().getId().equals(branchId))
					.toList();
			}
		}

		return ResponseEntity.ok(requests);
	}

	@GetMapping("/parent-requests/{id}")
	public ResponseEntity<ParentRequest> getParentRequest(@PathVariable Long id) {
		return parentRequestService.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PutMapping("/parent-requests/{id}/accept")
	public ResponseEntity<?> acceptParentRequest(
		@PathVariable Long id,
		@RequestBody(required = false) AcceptRequestRequest req
	) {
		Optional<ParentRequest> requestOpt = parentRequestService.findById(id);
		if (requestOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		ParentRequest request = requestOpt.get();
		if (!"pending".equals(request.getStatus())) {
			return ResponseEntity.badRequest().body(Map.of("error", "Request is not pending"));
		}

		// If bus and stop are provided, use them; otherwise auto-assign logic can be added
		if (req != null && req.getBusId() != null && req.getStopId() != null) {
			Bus bus = busRepository.findById(req.getBusId()).orElse(null);
			Stop stop = stopRepository.findById(req.getStopId()).orElse(null);
			if (bus != null && stop != null) {
				// Use the auto-accept method from service
				ParentRequest accepted = parentRequestService.autoAccept(request, bus, stop);
				return ResponseEntity.ok(accepted);
			}
		}

		// Simple accept without assignment
		request.setStatus("accepted");
		parentRequestRepository.save(request);
		return ResponseEntity.ok(request);
	}

	@PutMapping("/parent-requests/{id}/reject")
	public ResponseEntity<?> rejectParentRequest(@PathVariable Long id) {
		Optional<ParentRequest> requestOpt = parentRequestService.findById(id);
		if (requestOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		ParentRequest request = requestOpt.get();
		request.setStatus("rejected");
		parentRequestRepository.save(request);
		return ResponseEntity.ok(request);
	}

	// ========== STUDENTS ==========
	@GetMapping("/students")
	public ResponseEntity<List<Student>> getAllStudents(
		@RequestParam(required = false) Long branchId,
		@RequestParam(required = false) Long busId
	) {
		List<Student> students = studentRepository.findAll();
		if (branchId != null) {
			Branch branch = branchRepository.findById(branchId).orElse(null);
			if (branch != null) {
				students = studentRepository.findByBranch(branch);
			}
		}
		if (busId != null) {
			students = students.stream()
				.filter(s -> s.getBus() != null && s.getBus().getId().equals(busId))
				.toList();
		}
		return ResponseEntity.ok(students);
	}

	@GetMapping("/students/{id}")
	public ResponseEntity<Student> getStudent(@PathVariable Long id) {
		return studentRepository.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// ========== TRACKING ==========
	@GetMapping("/tracking/bus/{busId}")
	public ResponseEntity<?> getBusTracking(
		@PathVariable Long busId,
		@RequestParam(defaultValue = "10") int limit
	) {
		Bus bus = busRepository.findById(busId).orElse(null);
		if (bus == null) {
			return ResponseEntity.notFound().build();
		}

		List<TrackingPing> pings = trackingPingRepository.findByBusOrderByCreatedAtDesc(bus);
		if (limit > 0 && pings.size() > limit) {
			pings = pings.subList(0, limit);
		}

		return ResponseEntity.ok(Map.of(
			"busId", busId,
			"busNumber", bus.getBusNumber(),
			"trackingPings", pings,
			"latest", pings.isEmpty() ? null : pings.get(0)
		));
	}

	@GetMapping("/tracking/active")
	public ResponseEntity<?> getActiveTracking() {
		// Get all buses with recent tracking (last 10 minutes)
		List<Bus> allBuses = busRepository.findAll();
		Map<String, Object> activeTracking = allBuses.stream()
			.filter(bus -> {
				Optional<TrackingPing> lastPing = trackingPingRepository.findFirstByBusOrderByCreatedAtDesc(bus);
				return lastPing.isPresent() && 
					lastPing.get().getCreatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(10));
			})
			.collect(java.util.stream.Collectors.toMap(
				bus -> bus.getId().toString(),
				bus -> {
					Optional<TrackingPing> lastPing = trackingPingRepository.findFirstByBusOrderByCreatedAtDesc(bus);
					return Map.of(
						"busId", bus.getId(),
						"busNumber", bus.getBusNumber(),
						"location", lastPing.map(p -> Map.of(
							"latitude", p.getLatitude(),
							"longitude", p.getLongitude(),
							"speed", p.getSpeed() != null ? p.getSpeed() : 0,
							"heading", p.getHeading() != null ? p.getHeading() : 0,
							"timestamp", p.getCreatedAt()
						)).orElse(null)
					);
				}
			));

		return ResponseEntity.ok(activeTracking);
	}

	// ========== ALERTS ==========
	@GetMapping("/alerts")
	public ResponseEntity<Page<AlertLog>> getAlerts(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(alertLogRepository.findAll(pageable));
	}

	@GetMapping("/alerts/recent")
	public ResponseEntity<List<AlertLog>> getRecentAlerts(
		@RequestParam(defaultValue = "24") int hours
	) {
		java.time.LocalDateTime since = java.time.LocalDateTime.now().minusHours(hours);
		List<AlertLog> alerts = alertLogRepository.findAll().stream()
			.filter(alert -> alert.getSentAt() != null && alert.getSentAt().isAfter(since))
			.toList();
		return ResponseEntity.ok(alerts);
	}

	// ========== DRIVERS ==========
	@GetMapping("/drivers")
	public ResponseEntity<List<SchoolDriver>> getAllDrivers() {
		return ResponseEntity.ok(schoolDriverRepository.findAll());
	}

	@GetMapping("/drivers/{id}")
	public ResponseEntity<SchoolDriver> getDriver(@PathVariable Long id) {
		return schoolDriverRepository.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// ========== STATISTICS ==========
	@GetMapping("/stats")
	public ResponseEntity<?> getStatistics() {
		long totalInstitutions = institutionRepository.count();
		long totalBranches = branchRepository.count();
		long totalBuses = busRepository.count();
		long activeBuses = busRepository.findAll().stream()
			.filter(bus -> busActivationService.isBusActive(bus))
			.count();
		long totalStudents = studentRepository.count();
		long pendingRequests = parentRequestRepository.count() - 
			parentRequestRepository.findByStatus("accepted").size() -
			parentRequestRepository.findByStatus("rejected").size();
		long totalAlerts = alertLogRepository.count();
		long recentAlerts = alertLogRepository.findAll().stream()
			.filter(alert -> alert.getSentAt() != null && 
				alert.getSentAt().isAfter(java.time.LocalDateTime.now().minusHours(24)))
			.count();

		return ResponseEntity.ok(Map.of(
			"institutions", totalInstitutions,
			"branches", totalBranches,
			"buses", Map.of(
				"total", totalBuses,
				"active", activeBuses
			),
			"students", totalStudents,
			"parentRequests", Map.of(
				"pending", pendingRequests
			),
			"alerts", Map.of(
				"total", totalAlerts,
				"last24Hours", recentAlerts
			)
		));
	}

	@Data
	public static class AcceptRequestRequest {
		private Long busId;
		private Long stopId;
	}
}

