package com.ridefast.ride_fast_backend.service.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Route;
import com.ridefast.ride_fast_backend.model.school.Stop;
import com.ridefast.ride_fast_backend.repository.school.RouteRepository;
import com.ridefast.ride_fast_backend.repository.school.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RouteService {

	private final RouteRepository routeRepository;
	private final StopRepository stopRepository;

	public Route createRoute(Branch branch, String name, Boolean isMorning) {
		Route r = new Route();
		r.setBranch(branch);
		r.setName(name);
		r.setIsMorning(isMorning);
		return routeRepository.save(r);
	}

	public Optional<Route> findRoute(Long routeId) {
		return routeRepository.findById(routeId);
	}

	public Stop addStop(Route route, Stop stop) {
		stop.setRoute(route);
		return stopRepository.save(stop);
	}

	public List<Stop> listStops(Route route) {
		return stopRepository.findByRouteOrderByStopOrderAsc(route);
	}
}


