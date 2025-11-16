package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.v2.Settings;
import com.ridefast.ride_fast_backend.repository.v2.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/business-config")
@RequiredArgsConstructor
public class BusinessConfigController {

    private final SettingsRepository settingsRepository;

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String key) {
        Optional<Settings> s = settingsRepository.findByKeyName(key);
        if (s.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(Map.of(
                "key", s.get().getKeyName(),
                "mode", s.get().getMode(),
                "type", s.get().getSettingsType(),
                "liveValues", s.get().getLiveValues(),
                "testValues", s.get().getTestValues(),
                "active", s.get().getIsActive()
        ));
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> upsert(@PathVariable String key, @RequestBody Map<String, Object> body) {
        Settings s = settingsRepository.findByKeyName(key).orElseGet(() -> {
            Settings ns = new Settings();
            ns.setId(java.util.UUID.randomUUID().toString());
            ns.setKeyName(key);
            ns.setCreatedAt(LocalDateTime.now());
            return ns;
        });
        if (body.containsKey("mode")) s.setMode(String.valueOf(body.get("mode")));
        if (body.containsKey("type")) s.setSettingsType(String.valueOf(body.get("type")));
        if (body.containsKey("liveValues")) s.setLiveValues(String.valueOf(body.get("liveValues")));
        if (body.containsKey("testValues")) s.setTestValues(String.valueOf(body.get("testValues")));
        if (body.containsKey("active")) s.setIsActive(Boolean.valueOf(String.valueOf(body.get("active"))));
        s.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(s);
        return new ResponseEntity<>(Map.of("status", "ok"), HttpStatus.OK);
    }
}


