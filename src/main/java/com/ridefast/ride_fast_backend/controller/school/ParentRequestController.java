package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.ParentRequest;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.repository.school.BranchRepository;
import com.ridefast.ride_fast_backend.service.school.ParentRequestService;
import com.ridefast.ride_fast_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ParentRequestController {

	private final ParentRequestService parentRequestService;
	private final BranchRepository branchRepository;
	private final UserService userService;

	@PostMapping("/parent_requests")
	public ResponseEntity<?> createParentRequest(
		@RequestHeader(value = "Authorization", required = false) String jwtToken,
		@RequestBody @Valid CreateParentRequestRequest req
	) {
		try {
			// Get parent user from token or create if not provided
			MyUser parentUser = null;
			if (jwtToken != null && !jwtToken.isEmpty()) {
				try {
					parentUser = userService.getRequestedUserProfile(jwtToken);
				} catch (Exception e) {
					// Token invalid, continue without user
				}
			}

			Branch branch = branchRepository.findById(req.getBranchId()).orElse(null);
			if (branch == null) {
				return ResponseEntity.badRequest().body(Map.of("error", "Branch not found"));
			}

			ParentRequest request = new ParentRequest();
			request.setParentUser(parentUser);
			request.setBranch(branch);
			request.setStudentName(req.getStudentName());
			request.setStudentClass(req.getStudentClass());
			request.setSection(req.getSection());
			request.setAddress(req.getAddress());
			request.setParentPhone(req.getParentPhone());
			request.setParentEmail(req.getParentEmail());

			ParentRequest saved = parentRequestService.create(request);

			// Auto-accept logic (simplified - assign to first available bus/stop)
			// In production, this would have proper matching logic
			try {
				// For now, just save the request - auto-accept can be done via admin or background job
				return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
					"message", "Request created successfully. Will be processed shortly.",
					"requestId", saved.getId(),
					"status", saved.getStatus()
				));
			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
					"message", "Request created but auto-accept failed",
					"requestId", saved.getId(),
					"status", saved.getStatus(),
					"error", e.getMessage()
				));
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/parent_requests")
	public ResponseEntity<List<ParentRequest>> getAllRequests(
		@RequestParam(required = false) String status
	) {
		List<ParentRequest> requests;
		if (status != null) {
			requests = parentRequestService.findByStatus(status);
		} else {
			requests = parentRequestService.findAll();
		}
		return ResponseEntity.ok(requests);
	}

	@GetMapping("/parent_requests/{id}")
	public ResponseEntity<ParentRequest> getRequest(@PathVariable Long id) {
		return parentRequestService.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Data
	public static class CreateParentRequestRequest {
		private Long branchId;
		private String studentName;
		private String studentClass;
		private String section;
		private String address;
		private String parentPhone;
		private String parentEmail;
	}
}

