package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.Zone;
import com.ridefast.ride_fast_backend.repository.ZoneRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/zones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminZonesController {

  private final ZoneRepository zoneRepository;

  @GetMapping
  public ResponseEntity<List<Zone>> list() {
    return ResponseEntity.ok(zoneRepository.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Zone> get(@PathVariable String id) {
    Optional<Zone> z = zoneRepository.findById(id);
    return z.map(ResponseEntity::ok).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public ResponseEntity<Zone> create(@RequestBody Zone body) {
    if (body.getReadableId() == null || body.getReadableId().isBlank()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    if (body.getId() == null || body.getId().isBlank()) {
      body.setId(UUID.randomUUID().toString());
    }
    Zone saved = zoneRepository.save(body);
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Zone> update(@PathVariable String id, @RequestBody Zone body) {
    return zoneRepository.findById(id)
        .map(existing -> {
          if (body.getReadableId() != null) existing.setReadableId(body.getReadableId());
          if (body.getName() != null) existing.setName(body.getName());
          if (body.getPolygonWkt() != null) existing.setPolygonWkt(body.getPolygonWkt());
          if (body.getActive() != null) existing.setActive(body.getActive());
          return new ResponseEntity<>(zoneRepository.save(existing), HttpStatus.OK);
        })
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    if (!zoneRepository.existsById(id)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    zoneRepository.deleteById(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
