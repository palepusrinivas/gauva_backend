package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.driveraccess.DriverFeeConfiguration;
import com.ridefast.ride_fast_backend.repository.driveraccess.DriverFeeConfigurationRepository;
import com.ridefast.ride_fast_backend.service.driveraccess.DriverAccessRulesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/driver-access")
@RequiredArgsConstructor
public class AdminDriverAccessController {

    private final DriverFeeConfigurationRepository configRepository;
    private final DriverAccessRulesService rulesService;

    @GetMapping("/configurations")
    public ResponseEntity<List<DriverFeeConfiguration>> listConfigs() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    @GetMapping("/configurations/{vehicleType}")
    public ResponseEntity<DriverFeeConfiguration> getConfig(@PathVariable String vehicleType) {
        return configRepository.findByVehicleTypeIgnoreCase(vehicleType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/configurations")
    public ResponseEntity<DriverFeeConfiguration> createConfig(@RequestBody @Validated DriverFeeConfiguration body) {
        body.setId(null);
        return new ResponseEntity<>(configRepository.save(body), HttpStatus.CREATED);
    }

    @PutMapping("/configurations/{vehicleType}")
    public ResponseEntity<DriverFeeConfiguration> updateConfig(@PathVariable String vehicleType,
                                                               @RequestBody @Validated DriverFeeConfiguration body) {
        DriverFeeConfiguration existing = configRepository.findByVehicleTypeIgnoreCase(vehicleType).orElse(null);
        if (existing == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        body.setId(existing.getId());
        body.setVehicleType(existing.getVehicleType()); // keep key stable
        return ResponseEntity.ok(configRepository.save(body));
    }

    @PostMapping("/process-daily-fees")
    public ResponseEntity<Map<String, Object>> processDailyFees(@RequestParam(required = false) String date) {
        LocalDate target = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        return ResponseEntity.ok(rulesService.processDailyFees(target));
    }
}


