package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.Route;
import com.ridefast.ride_fast_backend.model.school.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StopRepository extends JpaRepository<Stop, Long> {
	List<Stop> findByRouteOrderByStopOrderAsc(Route route);
}


