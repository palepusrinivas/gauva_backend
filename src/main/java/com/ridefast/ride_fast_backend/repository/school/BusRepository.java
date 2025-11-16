package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.Branch;
import com.ridefast.ride_fast_backend.model.school.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusRepository extends JpaRepository<Bus, Long> {
	List<Bus> findByBranch(Branch branch);
}


