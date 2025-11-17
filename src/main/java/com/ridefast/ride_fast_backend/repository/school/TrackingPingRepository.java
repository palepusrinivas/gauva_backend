package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.Bus;
import com.ridefast.ride_fast_backend.model.school.TrackingPing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackingPingRepository extends JpaRepository<TrackingPing, Long> {
	List<TrackingPing> findByBusOrderByCreatedAtDesc(Bus bus);
	Optional<TrackingPing> findFirstByBusOrderByCreatedAtDesc(Bus bus);
}

