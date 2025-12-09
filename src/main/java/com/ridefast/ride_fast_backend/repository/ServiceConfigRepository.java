package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.ServiceConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceConfigRepository extends JpaRepository<ServiceConfig, Long> {

    Optional<ServiceConfig> findByServiceId(String serviceId);

    List<ServiceConfig> findByIsActiveTrue();

    List<ServiceConfig> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<ServiceConfig> findAllByOrderByDisplayOrderAsc();

    Page<ServiceConfig> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT s FROM ServiceConfig s WHERE " +
           "LOWER(s.name) LIKE '%' || LOWER(:search) || '%' OR " +
           "LOWER(s.serviceId) LIKE '%' || LOWER(:search) || '%' OR " +
           "LOWER(s.description) LIKE '%' || LOWER(:search) || '%'")
    Page<ServiceConfig> search(@Param("search") String search, Pageable pageable);

    boolean existsByServiceId(String serviceId);

    long countByIsActiveTrue();

    long countByIsActiveFalse();

    List<ServiceConfig> findByCategory(String category);

    List<ServiceConfig> findByVehicleType(String vehicleType);
}

