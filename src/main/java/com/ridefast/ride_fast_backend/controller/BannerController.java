package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.model.Banner;
import com.ridefast.ride_fast_backend.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public Banner Controller for mobile apps and user-facing applications.
 * Returns only active banners that are within their time period.
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerRepository bannerRepository;

    /**
     * Get all active banners for display
     * This endpoint is public and doesn't require authentication
     */
    @GetMapping
    public ResponseEntity<List<Banner>> getActiveBanners() {
        List<Banner> banners = bannerRepository.findActiveBannersInPeriod(LocalDateTime.now());
        return ResponseEntity.ok(banners);
    }

    /**
     * Get active banners ordered by display order
     */
    @GetMapping("/active")
    public ResponseEntity<List<Banner>> getActiveBannersOrdered() {
        List<Banner> banners = bannerRepository.findByActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(banners);
    }

    /**
     * Get a specific banner by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Banner> getBannerById(@PathVariable Long id) {
        return bannerRepository.findById(id)
                .filter(Banner::getActive)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

