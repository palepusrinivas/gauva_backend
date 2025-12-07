package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.enums.ServiceType;
import com.ridefast.ride_fast_backend.model.PricingProfile;
import com.ridefast.ride_fast_backend.model.ServiceRate;
import com.ridefast.ride_fast_backend.model.Zone;
import com.ridefast.ride_fast_backend.model.ZoneFareRule;
import com.ridefast.ride_fast_backend.repository.PricingProfileRepository;
import com.ridefast.ride_fast_backend.repository.ServiceRateRepository;
import com.ridefast.ride_fast_backend.repository.ZoneFareRuleRepository;
import com.ridefast.ride_fast_backend.repository.ZoneRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
public class AdminPricingController {

  private final PricingProfileRepository profileRepo;
  private final ServiceRateRepository rateRepo;
  private final ZoneRepository zoneRepo;
  private final ZoneFareRuleRepository zoneRuleRepo;

  // Profiles
  @GetMapping("/profiles")
  public ResponseEntity<List<PricingProfile>> listProfiles() {
    return ResponseEntity.ok(profileRepo.findAll());
  }

  @PostMapping("/profiles")
  public ResponseEntity<PricingProfile> upsertProfile(@RequestBody PricingProfile p) {
    if (p.getId() != null) {
      Optional<PricingProfile> existing = profileRepo.findById(p.getId());
      if (existing.isPresent()) {
        PricingProfile e = existing.get();
        e.setName(p.getName());
        e.setCurrency(p.getCurrency());
        e.setBaseFare(p.getBaseFare());
        e.setPerKmRate(p.getPerKmRate());
        e.setTimeRatePerMin(p.getTimeRatePerMin());
        e.setActive(p.isActive());
        return ResponseEntity.ok(profileRepo.save(e));
      }
    }
    return ResponseEntity.ok(profileRepo.save(p));
  }

  @PostMapping("/profiles/{id}/activate")
  @Transactional
  public ResponseEntity<PricingProfile> activate(@PathVariable Long id) {
    List<PricingProfile> all = profileRepo.findAll();
    PricingProfile active = null;
    for (PricingProfile p : all) {
      boolean isActive = p.getId().equals(id);
      p.setActive(isActive);
      if (isActive) active = p;
    }
    profileRepo.saveAll(all);
    return ResponseEntity.ok(active);
  }

  // Service Rates
  @GetMapping("/profiles/{profileId}/service-rates")
  public ResponseEntity<List<ServiceRate>> listRates(@PathVariable Long profileId) {
    PricingProfile profile = profileRepo.findById(profileId).orElseThrow();
    return ResponseEntity.ok(rateRepo.findAll().stream().filter(r -> r.getPricingProfile().getId().equals(profile.getId())).toList());
  }

  @PostMapping("/profiles/{profileId}/service-rates")
  public ResponseEntity<ServiceRate> upsertRate(@PathVariable Long profileId,
      @RequestParam ServiceType serviceType,
      @RequestParam double baseFare,
      @RequestParam double perKmRate,
      @RequestParam(required = false, defaultValue = "0") double timeRatePerMin) {
    PricingProfile profile = profileRepo.findById(profileId).orElseThrow();
    Optional<ServiceRate> existing = rateRepo.findByPricingProfileAndServiceType(profile, serviceType);
    ServiceRate rate = existing.orElseGet(() -> ServiceRate.builder()
        .pricingProfile(profile)
        .serviceType(serviceType)
        .baseFare(baseFare)
        .perKmRate(perKmRate)
        .timeRatePerMin(timeRatePerMin)
        .build());
    rate.setBaseFare(baseFare);
    rate.setPerKmRate(perKmRate);
    rate.setTimeRatePerMin(timeRatePerMin);
    return ResponseEntity.ok(rateRepo.save(rate));
  }

  // Zones
  @GetMapping("/zones")
  public ResponseEntity<List<Zone>> listZones() {
    return ResponseEntity.ok(zoneRepo.findAll());
  }

  @PostMapping("/zones")
  public ResponseEntity<Zone> upsertZone(@RequestBody Zone z) {
    if (z.getId() != null) {
      Zone e = zoneRepo.findById(z.getId()).orElseThrow();
      e.setReadableId(z.getReadableId());
      e.setName(z.getName());
      e.setPolygonWkt(z.getPolygonWkt());
      e.setActive(z.getActive());
      return ResponseEntity.ok(zoneRepo.save(e));
    }
    return ResponseEntity.ok(zoneRepo.save(z));
  }

  // Zone rules
  @GetMapping("/zones/{zoneId}/rules")
  public ResponseEntity<List<ZoneFareRule>> listZoneRules(@PathVariable Long zoneId) {
    Zone zone = zoneRepo.findById(zoneId).orElseThrow();
    return ResponseEntity.ok(zoneRuleRepo.findAll().stream().filter(r -> r.getZone().getId().equals(zone.getId())).toList());
  }

  @PostMapping("/zones/{zoneId}/rules")
  public ResponseEntity<ZoneFareRule> upsertZoneRule(@PathVariable Long zoneId,
      @RequestParam Long profileId,
      @RequestBody ZoneFareRule body) {
    Zone zone = zoneRepo.findById(zoneId).orElseThrow();
    PricingProfile profile = profileRepo.findById(profileId).orElseThrow();
    ZoneFareRule r;
    if (body.getId() != null) {
      r = zoneRuleRepo.findById(body.getId()).orElse(new ZoneFareRule());
    } else {
      r = new ZoneFareRule();
    }
    r.setZone(zone);
    r.setPricingProfile(profile);
    r.setBaseFareOverride(body.getBaseFareOverride());
    r.setPerKmRateOverride(body.getPerKmRateOverride());
    r.setTimeRatePerMinOverride(body.getTimeRatePerMinOverride());
    r.setExtraFareStatus(body.getExtraFareStatus());
    r.setExtraFareReason(body.getExtraFareReason());
    r.setActive(body.isActive());
    return ResponseEntity.ok(zoneRuleRepo.save(r));
  }
}
