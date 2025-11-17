package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Institution;
import com.ridefast.ride_fast_backend.repository.school.BranchRepository;
import com.ridefast.ride_fast_backend.repository.school.InstitutionRepository;
import com.ridefast.ride_fast_backend.service.school.BranchService;
import jakarta.validation.Valid;
import com.ridefast.ride_fast_backend.dto.school.CreateBranchRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BranchController {

	private final InstitutionRepository institutionRepository;
    @Autowired
	private final BranchService branchService;
    private final BranchRepository branchRepository;

	// Public endpoints (no admin required) - for parents to search branches
	@GetMapping("/branches")
	public ResponseEntity<List<Branch>> getAllBranches(
		@RequestParam(required = false) String search
	){
		List<Branch> branches;
		if (search != null && !search.trim().isEmpty()) {
			branches = branchRepository.findAll().stream()
				.filter(b -> b.getName() != null && b.getName().toLowerCase().contains(search.toLowerCase()))
				.toList();
		} else {
			branches = branchRepository.findAll();
		}
		return ResponseEntity.ok(branches);
	}

	@GetMapping("/branches/by_code")
	public ResponseEntity<Branch> getBranchByCode(@RequestParam String code) {
		return branchRepository.findByBranchId(code)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// Admin-only endpoints
	@PostMapping("/institutions/{institutionId}/branches")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Branch> create(@PathVariable Long institutionId, @RequestBody @Valid CreateBranchRequest req) {
		Institution institution = institutionRepository.findById(institutionId).orElse(null);
		if (institution == null) return ResponseEntity.notFound().build();
		Branch b = new Branch();
		b.setInstitution(institution);
		b.setName(req.getName());
        b.setBranchId(branchService.BranchuniqueId());
		b.setAddress(req.getAddress());
		b.setCity(req.getCity());
		b.setState(req.getState());
		b.setPincode(req.getPincode());
		b.setLatitude(req.getLatitude());
		b.setLongitude(req.getLongitude());
		b.setSubscriptionPlan(req.getSubscriptionPlan());
		Branch saved = branchService.create(b);
		return new ResponseEntity<>(saved, HttpStatus.CREATED);
	}

	@GetMapping("/institutions/{institutionId}/branches")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Branch>> list(@PathVariable Long institutionId) {
		Institution institution = institutionRepository.findById(institutionId).orElse(null);
		if (institution == null) return ResponseEntity.notFound().build();
		return ResponseEntity.ok(branchService.findByInstitution(institution));
	}
	@PutMapping("/branches/{branchId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Branch> updateBranch(@PathVariable Long branchId, @RequestBody CreateBranchRequest req) {
		Branch existing = branchRepository.findById(branchId).orElse(null);
		if (existing == null) return ResponseEntity.notFound().build();
		Branch updates = new Branch();
		updates.setName(req.getName());
		updates.setAddress(req.getAddress());
		updates.setCity(req.getCity());
		updates.setState(req.getState());
		updates.setPincode(req.getPincode());
		updates.setLatitude(req.getLatitude());
		updates.setLongitude(req.getLongitude());
		updates.setSubscriptionPlan(req.getSubscriptionPlan());
		return ResponseEntity.ok(branchService.update(existing, updates));
	}

	@DeleteMapping("/branches/{branchId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteBranch(@PathVariable Long branchId) {
		Branch existing = branchRepository.findById(branchId).orElse(null);
		if (existing == null) return ResponseEntity.notFound().build();
		branchService.delete(existing);
		return ResponseEntity.noContent().build();
	}

	
}


