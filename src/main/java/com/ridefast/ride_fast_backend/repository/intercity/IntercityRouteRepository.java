package com.ridefast.ride_fast_backend.repository.intercity;

import com.ridefast.ride_fast_backend.model.intercity.IntercityRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntercityRouteRepository extends JpaRepository<IntercityRoute, Long> {
    
    Optional<IntercityRoute> findByRouteCode(String routeCode);
    
    List<IntercityRoute> findByIsActiveTrue();
    
    List<IntercityRoute> findByOriginNameIgnoreCaseAndIsActiveTrue(String originName);
    
    List<IntercityRoute> findByDestinationNameIgnoreCaseAndIsActiveTrue(String destinationName);
    
    boolean existsByRouteCode(String routeCode);
    
    /**
     * Find routes near given origin and destination coordinates
     */
    @Query("""
        SELECT r FROM IntercityRoute r 
        WHERE r.isActive = true 
        AND (
            (6371 * acos(cos(radians(:originLat)) * cos(radians(r.originLatitude)) 
            * cos(radians(r.originLongitude) - radians(:originLng)) 
            + sin(radians(:originLat)) * sin(radians(r.originLatitude)))) < :radiusKm
        )
        AND (
            (6371 * acos(cos(radians(:destLat)) * cos(radians(r.destinationLatitude)) 
            * cos(radians(r.destinationLongitude) - radians(:destLng)) 
            + sin(radians(:destLat)) * sin(radians(r.destinationLatitude)))) < :radiusKm
        )
        """)
    List<IntercityRoute> findNearbyRoutes(
        @Param("originLat") Double originLat,
        @Param("originLng") Double originLng,
        @Param("destLat") Double destLat,
        @Param("destLng") Double destLng,
        @Param("radiusKm") Double radiusKm
    );
}

