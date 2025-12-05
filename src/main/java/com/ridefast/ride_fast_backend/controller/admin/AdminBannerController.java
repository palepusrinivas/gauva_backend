package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.Banner;
import com.ridefast.ride_fast_backend.repository.BannerRepository;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminBannerController {

    private final BannerRepository bannerRepository;
    private final StorageService storageService;

    /**
     * Get all banners with optional filtering and pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllBanners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Banner> banners;
        if (search != null && !search.trim().isEmpty()) {
            banners = bannerRepository.searchByTitle(search.trim(), pageable);
        } else if (active != null) {
            banners = bannerRepository.findByActive(active, pageable);
        } else {
            banners = bannerRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(banners);
    }

    /**
     * Get all banners as a simple list (no pagination)
     */
    @GetMapping("/list")
    public ResponseEntity<List<Banner>> getBannerList(
            @RequestParam(required = false) Boolean active) {
        
        List<Banner> banners;
        if (active != null) {
            banners = active ? bannerRepository.findByActiveTrue() : bannerRepository.findByActiveFalse();
        } else {
            banners = bannerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        
        return ResponseEntity.ok(banners);
    }

    /**
     * Get banner by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Banner> getBannerById(@PathVariable Long id) {
        return bannerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new banner with image upload
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBanner(
            @RequestParam("title") String title,
            @RequestParam(value = "shortDescription", required = false) String shortDescription,
            @RequestParam(value = "redirectLink", required = false) String redirectLink,
            @RequestParam(value = "timePeriod", required = false) String timePeriod,
            @RequestParam(value = "active", defaultValue = "true") Boolean active,
            @RequestParam(value = "displayOrder", defaultValue = "0") Integer displayOrder,
            @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage) {
        
        try {
            // Validate title
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
            }
            
            if (title.length() > 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title must be less than 100 characters"));
            }

            String imageUrl = null;
            
            // Handle image upload
            if (bannerImage != null && !bannerImage.isEmpty()) {
                // Validate file type
                String contentType = bannerImage.getContentType();
                if (contentType == null || !isValidImageType(contentType)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid image type. Allowed: jpg, jpeg, png, webp"));
                }
                
                // Validate file size (5MB max)
                if (bannerImage.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Image size must be less than 5MB"));
                }
                
                // Upload to storage
                try {
                    String fileName = "banners/" + UUID.randomUUID() + "_" + bannerImage.getOriginalFilename();
                    imageUrl = storageService.uploadFile(bannerImage, fileName);
                    log.info("Banner image uploaded: {}", imageUrl);
                } catch (Exception e) {
                    log.error("Failed to upload banner image", e);
                    // Continue without image if upload fails
                }
            }

            // Create banner
            Banner banner = Banner.builder()
                    .title(title.trim())
                    .shortDescription(shortDescription != null ? shortDescription.trim() : null)
                    .redirectLink(redirectLink != null ? redirectLink.trim() : null)
                    .timePeriod(timePeriod)
                    .imageUrl(imageUrl)
                    .active(active)
                    .displayOrder(displayOrder)
                    .build();

            Banner savedBanner = bannerRepository.save(banner);
            log.info("Banner created: id={}, title={}", savedBanner.getId(), savedBanner.getTitle());
            
            return new ResponseEntity<>(savedBanner, HttpStatus.CREATED);
            
        } catch (Exception e) {
            log.error("Error creating banner", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create banner: " + e.getMessage()));
        }
    }

    /**
     * Update banner
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBanner(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "shortDescription", required = false) String shortDescription,
            @RequestParam(value = "redirectLink", required = false) String redirectLink,
            @RequestParam(value = "timePeriod", required = false) String timePeriod,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage) {
        
        return bannerRepository.findById(id)
                .map(banner -> {
                    try {
                        // Update fields if provided
                        if (title != null && !title.trim().isEmpty()) {
                            if (title.length() > 100) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Title must be less than 100 characters"));
                            }
                            banner.setTitle(title.trim());
                        }
                        if (shortDescription != null) {
                            banner.setShortDescription(shortDescription.trim());
                        }
                        if (redirectLink != null) {
                            banner.setRedirectLink(redirectLink.trim());
                        }
                        if (timePeriod != null) {
                            banner.setTimePeriod(timePeriod);
                        }
                        if (active != null) {
                            banner.setActive(active);
                        }
                        if (displayOrder != null) {
                            banner.setDisplayOrder(displayOrder);
                        }

                        // Handle image upload
                        if (bannerImage != null && !bannerImage.isEmpty()) {
                            String contentType = bannerImage.getContentType();
                            if (contentType == null || !isValidImageType(contentType)) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Invalid image type"));
                            }
                            
                            if (bannerImage.getSize() > 5 * 1024 * 1024) {
                                return ResponseEntity.badRequest().body(Map.of("error", "Image size must be less than 5MB"));
                            }

                            try {
                                String fileName = "banners/" + UUID.randomUUID() + "_" + bannerImage.getOriginalFilename();
                                String imageUrl = storageService.uploadFile(bannerImage, fileName);
                                banner.setImageUrl(imageUrl);
                            } catch (Exception e) {
                                log.error("Failed to upload banner image", e);
                            }
                        }

                        Banner updatedBanner = bannerRepository.save(banner);
                        log.info("Banner updated: id={}", updatedBanner.getId());
                        return ResponseEntity.ok(updatedBanner);
                        
                    } catch (Exception e) {
                        log.error("Error updating banner", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Failed to update banner"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update banner status only
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBannerStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        
        return bannerRepository.findById(id)
                .map(banner -> {
                    banner.setActive(active);
                    Banner updatedBanner = bannerRepository.save(banner);
                    log.info("Banner status updated: id={}, active={}", id, active);
                    return ResponseEntity.ok(updatedBanner);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete banner
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable Long id) {
        return bannerRepository.findById(id)
                .map(banner -> {
                    // Optionally delete image from storage
                    if (banner.getImageUrl() != null) {
                        try {
                            // storageService.deleteFile(banner.getImageUrl());
                        } catch (Exception e) {
                            log.warn("Failed to delete banner image", e);
                        }
                    }
                    
                    bannerRepository.delete(banner);
                    log.info("Banner deleted: id={}", id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get banner statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBannerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", bannerRepository.count());
        stats.put("active", bannerRepository.countByActiveTrue());
        stats.put("inactive", bannerRepository.countByActiveFalse());
        return ResponseEntity.ok(stats);
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/webp");
    }
}

