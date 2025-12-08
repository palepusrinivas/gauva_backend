package com.ridefast.ride_fast_backend.repository.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityTripStatus;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.model.intercity.IntercityTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntercityTripRepository extends JpaRepository<IntercityTrip, Long> {
    
    Optional<IntercityTrip> findByTripCode(String tripCode);
    
    boolean existsByTripCode(String tripCode);
    
    /**
     * Find trips available for joining (share pool)
     */
    @Query("""
        SELECT t FROM IntercityTrip t 
        WHERE t.status IN :statuses 
        AND t.isPrivate = false 
        AND t.seatsBooked < t.totalSeats
        AND t.vehicleType = :vehicleType
        AND t.scheduledDeparture > :now
        ORDER BY t.scheduledDeparture ASC
        """)
    List<IntercityTrip> findAvailableTripsForPooling(
        @Param("statuses") List<IntercityTripStatus> statuses,
        @Param("vehicleType") IntercityVehicleType vehicleType,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Find all available trips for a route
     */
    @Query("""
        SELECT t FROM IntercityTrip t 
        WHERE t.route.id = :routeId 
        AND t.status IN :statuses 
        AND t.isPrivate = false 
        AND t.seatsBooked < t.totalSeats
        AND t.scheduledDeparture > :now
        ORDER BY t.scheduledDeparture ASC
        """)
    List<IntercityTrip> findAvailableTripsForRoute(
        @Param("routeId") Long routeId,
        @Param("statuses") List<IntercityTripStatus> statuses,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Find trips that have passed countdown expiry but not yet processed
     */
    @Query("""
        SELECT t FROM IntercityTrip t 
        WHERE t.status = :status 
        AND t.countdownExpiry < :now
        """)
    List<IntercityTrip> findTripsWithExpiredCountdown(
        @Param("status") IntercityTripStatus status,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Find trips by driver
     */
    List<IntercityTrip> findByDriverIdAndStatusIn(Long driverId, List<IntercityTripStatus> statuses);
    
    /**
     * Find trips by status
     */
    List<IntercityTrip> findByStatusOrderByScheduledDepartureAsc(IntercityTripStatus status);
    
    /**
     * Count trips by status for admin dashboard
     */
    @Query("SELECT t.status, COUNT(t) FROM IntercityTrip t GROUP BY t.status")
    List<Object[]> countByStatus();
    
    /**
     * Find trips by route, driver, and status (for return trip guarantee)
     */
    @Query("""
        SELECT t FROM IntercityTrip t 
        WHERE (:routeId IS NULL OR t.route.id = :routeId)
        AND (:driverId IS NULL OR t.driver.id = :driverId)
        AND t.status IN :statuses
        """)
    List<IntercityTrip> findByRouteIdAndDriverIdAndStatusIn(
        @Param("routeId") Long routeId,
        @Param("driverId") Long driverId,
        @Param("statuses") List<IntercityTripStatus> statuses
    );
}

