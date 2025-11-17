package com.ridefast.ride_fast_backend.repository.school;

import com.ridefast.ride_fast_backend.model.school.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
}

