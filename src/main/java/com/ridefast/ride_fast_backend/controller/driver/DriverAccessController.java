package com.ridefast.ride_fast_backend.controller.driver;

import com.ridefast.ride_fast_backend.service.driveraccess.DriverAccessRulesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/driver/access")
@RequiredArgsConstructor
public class DriverAccessController {

    private final DriverAccessRulesService service;

    @GetMapping("/fee-configurations")
    public ResponseEntity<Map<String, Object>> feeConfigurations() {
        return ResponseEntity.ok(service.getFeeConfigurations());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam Long driverId,
                                                      @RequestParam String vehicle_type) {
        return ResponseEntity.ok(service.todayStatus(driverId, vehicle_type));
    }

    @GetMapping("/can-accept-trips")
    public ResponseEntity<Map<String, Object>> canAccept(@RequestParam Long driverId,
                                                         @RequestParam String vehicle_type) {
        return ResponseEntity.ok(service.canAcceptTrips(driverId, vehicle_type));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics(@RequestParam Long driverId,
                                                          @RequestParam("start_date") String startDate,
                                                          @RequestParam("end_date") String endDate) {
        return ResponseEntity.ok(service.statistics(driverId, startDate, endDate));
    }

    @PostMapping("/record-trip-complete")
    public ResponseEntity<Void> recordTripComplete(@RequestParam Long driverId,
                                                   @RequestParam Long trip_id,
                                                   @RequestParam String vehicle_type) {
        service.recordTripCompleted(driverId, trip_id, vehicle_type);
        return ResponseEntity.ok().build();
    }
}


