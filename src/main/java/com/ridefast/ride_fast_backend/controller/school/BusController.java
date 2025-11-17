package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.SchoolDriver;
import com.ridefast.ride_fast_backend.repository.school.BranchRepository;
import com.ridefast.ride_fast_backend.service.school.BusService;
import com.ridefast.ride_fast_backend.service.school.BusActivationService;
import jakarta.validation.Valid;
import com.ridefast.ride_fast_backend.dto.school.CreateBusRequest;
import com.ridefast.ride_fast_backend.dto.school.AssignDriverRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BusController {

	private final BranchRepository branchRepository;
	private final BusService busService;
	private final BusActivationService busActivationService;

	@PostMapping("/branches/{branchId}/buses")
	public ResponseEntity<Bus> create(@PathVariable Long branchId, @RequestBody @Valid CreateBusRequest req) {
		Branch branch = branchRepository.findById(branchId).orElse(null);
		if (branch == null) return ResponseEntity.notFound().build();
		Bus b = new Bus();
		b.setBranch(branch);
		b.setBusNumber(req.getBusNumber());
		b.setCapacity(req.getCapacity());
		b.setType(req.getType());
		b.setRcExpiry(req.getRcExpiry());
		b.setInsuranceExpiry(req.getInsuranceExpiry());
		b.setPhotoUrl(req.getPhotoUrl());
		Bus saved = busService.create(b);
		return new ResponseEntity<>(saved, HttpStatus.CREATED);
	}

	@GetMapping("/branches/{branchId}/buses")
	public ResponseEntity<List<Bus>> list(@PathVariable Long branchId) {
		Branch branch = branchRepository.findById(branchId).orElse(null);
		if (branch == null) return ResponseEntity.notFound().build();
		return ResponseEntity.ok(busService.listByBranch(branch));
	}

	@PutMapping("/buses/{busId}")
	public ResponseEntity<Bus> update(@PathVariable Long busId, @RequestBody CreateBusRequest req) {
		Bus existing = busService.findById(busId).orElse(null);
		if (existing == null) return ResponseEntity.notFound().build();
		Bus updates = new Bus();
		updates.setBusNumber(req.getBusNumber());
		updates.setCapacity(req.getCapacity());
		updates.setType(req.getType());
		updates.setRcExpiry(req.getRcExpiry());
		updates.setInsuranceExpiry(req.getInsuranceExpiry());
		updates.setPhotoUrl(req.getPhotoUrl());
		return ResponseEntity.ok(busService.update(existing, updates));
	}

	@DeleteMapping("/buses/{busId}")
	public ResponseEntity<?> delete(@PathVariable Long busId) {
		Bus existing = busService.findById(busId).orElse(null);
		if (existing == null) return ResponseEntity.notFound().build();
		busService.delete(existing);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/buses/{busId}/drivers")
	public ResponseEntity<SchoolDriver> assignDriver(@PathVariable Long busId, @RequestBody @Valid AssignDriverRequest req) {
		Bus bus = busService.findById(busId).orElse(null);
		if (bus == null) return ResponseEntity.notFound().build();
		SchoolDriver driver = busService.findDriverById(req.getDriverId()).orElse(null);
		if (driver == null) return ResponseEntity.notFound().build();
		SchoolDriver saved = busService.assignDriver(bus, driver);
		return ResponseEntity.ok(saved);
	}

	@PostMapping("/buses/{busId}/activate")
	public ResponseEntity<?> activateBus(@PathVariable Long busId) {
		Bus bus = busService.findById(busId).orElse(null);
		if (bus == null) return ResponseEntity.notFound().build();
		
		com.ridefast.ride_fast_backend.model.school.BusActivation activation = busActivationService.activateBus(bus);
		return ResponseEntity.ok(Map.of(
			"message", "Bus activated successfully",
			"busId", busId,
			"activationId", activation.getId(),
			"fee", activation.getActivationFee(),
			"startDate", activation.getStartDate(),
			"endDate", activation.getEndDate(),
			"status", activation.getStatus()
		));
	}

	
}


