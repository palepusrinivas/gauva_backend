package com.ridefast.ride_fast_backend.controller.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Route;
import com.ridefast.ride_fast_backend.model.school.Stop;
import com.ridefast.ride_fast_backend.repository.school.BranchRepository;
import com.ridefast.ride_fast_backend.service.school.RouteService;
import jakarta.validation.Valid;
import com.ridefast.ride_fast_backend.dto.school.CreateRouteRequest;
import com.ridefast.ride_fast_backend.dto.school.CreateStopRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RouteController {

	private final BranchRepository branchRepository;
	private final RouteService routeService;

	@PostMapping("/branches/{branchId}/routes")
	public ResponseEntity<Route> createRoute(@PathVariable Long branchId, @RequestBody @Valid CreateRouteRequest req) {
		Branch branch = branchRepository.findById(branchId).orElse(null);
		if (branch == null) return ResponseEntity.notFound().build();
		return new ResponseEntity<>(routeService.createRoute(branch, req.getName(), req.getIsMorning()), HttpStatus.CREATED);
	}

	@PostMapping("/routes/{routeId}/stops")
	public ResponseEntity<Stop> addStop(@PathVariable Long routeId, @RequestBody @Valid CreateStopRequest req) {
		Route route = routeService.findRoute(routeId).orElse(null);
		if (route == null) return ResponseEntity.notFound().build();
		Stop s = new Stop();
		s.setName(req.getName());
		s.setLatitude(req.getLatitude());
		s.setLongitude(req.getLongitude());
		s.setStopOrder(req.getStopOrder());
		s.setEtaMinutesFromPrev(req.getEtaMinutesFromPrev());
		return new ResponseEntity<>(routeService.addStop(route, s), HttpStatus.CREATED);
	}

	@GetMapping("/routes/{routeId}/stops")
	public ResponseEntity<List<Stop>> listStops(@PathVariable Long routeId) {
		Route route = routeService.findRoute(routeId).orElse(null);
		if (route == null) return ResponseEntity.notFound().build();
		return ResponseEntity.ok(routeService.listStops(route));
	}

	
}


