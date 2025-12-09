package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Find all active banners
    List<Banner> findByActiveTrue();

    // Find all inactive banners
    List<Banner> findByActiveFalse();

    // Find active banners ordered by display order
    List<Banner> findByActiveTrueOrderByDisplayOrderAsc();

    // Find banners by active status with pagination
    Page<Banner> findByActive(Boolean active, Pageable pageable);

    // Find active banners that are within their time period
    @Query("SELECT b FROM Banner b WHERE b.active = true " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.displayOrder ASC")
    List<Banner> findActiveBannersInPeriod(@Param("now") LocalDateTime now);

    // Search banners by title
    @Query("SELECT b FROM Banner b WHERE LOWER(b.title) LIKE '%' || LOWER(:search) || '%'")
    Page<Banner> searchByTitle(@Param("search") String search, Pageable pageable);

    // Count active banners
    long countByActiveTrue();

    // Count inactive banners
    long countByActiveFalse();
}

