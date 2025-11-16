package com.ridefast.ride_fast_backend.service.driveraccess;

import java.time.LocalDate;
import java.util.Map;

public interface DriverAccessRulesService {
    Map<String, Object> getFeeConfigurations();
    Map<String, Object> todayStatus(Long driverId, String vehicleType);
    Map<String, Object> canAcceptTrips(Long driverId, String vehicleType);
    void recordTripCompleted(Long driverId, Long rideId, String vehicleType);
    void recordCustomerCancelledAfterStart(Long driverId, Long rideId, String vehicleType);
    void recordDriverCancellation(Long driverId, Long rideId, String vehicleType);
    Map<String, Object> statistics(Long driverId, String startDate, String endDate);
    Map<String, Object> processDailyFees(LocalDate date);
}


