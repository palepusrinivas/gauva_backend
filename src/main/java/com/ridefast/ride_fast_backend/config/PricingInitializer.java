package com.ridefast.ride_fast_backend.config;

import com.ridefast.ride_fast_backend.enums.ServiceType;
import com.ridefast.ride_fast_backend.model.PricingProfile;
import com.ridefast.ride_fast_backend.model.ServiceRate;
import com.ridefast.ride_fast_backend.repository.PricingProfileRepository;
import com.ridefast.ride_fast_backend.repository.ServiceRateRepository;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingInitializer {

  @Bean
  CommandLineRunner seedPricing(PricingProfileRepository profileRepo, ServiceRateRepository rateRepo) {
    return args -> {
      PricingProfile profile = profileRepo.findFirstByActiveTrue().orElseGet(() -> {
        PricingProfile p = PricingProfile.builder()
            .name("Default INR Profile")
            .currency("INR")
            .baseFare(50.0)
            .perKmRate(13.0)
            .timeRatePerMin(0.0)
            .active(true)
            .build();
        return profileRepo.save(p);
      });

      // Seed default service rates if not present
      Map<ServiceType, double[]> defaults = new EnumMap<>(ServiceType.class);
      defaults.put(ServiceType.MEGA, new double[]{50.0, 15.0});
      defaults.put(ServiceType.SMALL_SEDAN, new double[]{150.0, 30.0});
      defaults.put(ServiceType.BIKE, new double[]{22.0, 7.0});
      defaults.put(ServiceType.CAR, new double[]{200.0, 33.0});

      for (Map.Entry<ServiceType, double[]> e : defaults.entrySet()) {
        ServiceType type = e.getKey();
        double base = e.getValue()[0];
        double perKm = e.getValue()[1];
        rateRepo.findByPricingProfileAndServiceType(profile, type).orElseGet(() -> {
          ServiceRate rate = ServiceRate.builder()
              .pricingProfile(profile)
              .serviceType(type)
              .baseFare(base)
              .perKmRate(perKm)
              .timeRatePerMin(0.0)
              .build();
          return rateRepo.save(rate);
        });
      }
    };
  }
}
