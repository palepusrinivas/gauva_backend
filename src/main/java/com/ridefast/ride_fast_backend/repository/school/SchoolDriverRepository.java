package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.SchoolDriver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolDriverRepository extends JpaRepository<SchoolDriver, Long> {
	Optional<SchoolDriver> findByPhone(String phone);
}


