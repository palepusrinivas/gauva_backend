package com.ridefast.ride_fast_backend.repository.driveraccess;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.driveraccess.DriverDailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DriverDailyActivityRepository extends JpaRepository<DriverDailyActivity, Long> {
    Optional<DriverDailyActivity> findByDriverAndActivityDate(Driver driver, LocalDate date);
    List<DriverDailyActivity> findByDriverAndActivityDateBetweenOrderByActivityDateAsc(Driver driver, LocalDate start, LocalDate end);
}


