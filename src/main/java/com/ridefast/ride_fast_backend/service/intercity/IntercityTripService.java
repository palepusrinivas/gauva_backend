package com.ridefast.ride_fast_backend.service.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.IntercityTripDTO;
import com.ridefast.ride_fast_backend.enums.IntercityTripStatus;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.intercity.IntercityRoute;
import com.ridefast.ride_fast_backend.model.intercity.IntercityTrip;
import com.ridefast.ride_fast_backend.model.intercity.IntercityVehicleConfig;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityBookingRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityRouteRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityTripRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityVehicleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing intercity trips
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntercityTripService {
    
    private final IntercityTripRepository tripRepository;
    private final IntercityRouteRepository routeRepository;
    private final IntercityVehicleConfigRepository vehicleConfigRepository;
    private final IntercityBookingRepository bookingRepository;
    private final IntercityPricingService pricingService;
    
    /** Default countdown duration in minutes */
    private static final int DEFAULT_COUNTDOWN_MINUTES = 10;
    
    /**
     * Create a new trip for booking
     */
    @Transactional
    public IntercityTrip createTrip(
            Long routeId,
            IntercityVehicleType vehicleType,
            String pickupAddress,
            Double pickupLat,
            Double pickupLng,
            String dropAddress,
            Double dropLat,
            Double dropLng,
            LocalDateTime scheduledDeparture,
            boolean isPrivate
    ) throws ResourceNotFoundException {
        IntercityVehicleConfig config = vehicleConfigRepository.findByVehicleType(vehicleType)
            .orElseThrow(() -> new ResourceNotFoundException("VehicleConfig", "type", vehicleType.name()));
        
        IntercityRoute route = null;
        if (routeId != null) {
            route = routeRepository.findById(routeId).orElse(null);
        }
        
        // Apply price multiplier if route exists
        BigDecimal totalPrice = config.getTotalPrice();
        if (route != null && route.getPriceMultiplier() != null) {
            totalPrice = totalPrice.multiply(route.getPriceMultiplier());
        }
        
        // Generate unique trip code
        String tripCode = generateTripCode();
        
        IntercityTrip trip = IntercityTrip.builder()
            .tripCode(tripCode)
            .route(route)
            .vehicleType(vehicleType)
            .vehicleConfig(config)
            .status(IntercityTripStatus.PENDING)
            .totalSeats(config.getMaxSeats())
            .seatsBooked(0)
            .minSeats(isPrivate ? config.getMaxSeats() : config.getMinSeats())
            .totalPrice(totalPrice)
            .currentPerHeadPrice(totalPrice)
            .scheduledDeparture(scheduledDeparture != null ? scheduledDeparture : LocalDateTime.now().plusMinutes(30))
            .pickupAddress(pickupAddress)
            .pickupLatitude(pickupLat)
            .pickupLongitude(pickupLng)
            .dropAddress(dropAddress)
            .dropLatitude(dropLat)
            .dropLongitude(dropLng)
            .isPrivate(isPrivate)
            .build();
        
        return tripRepository.save(trip);
    }
    
    /**
     * Create a trip published by driver with all options
     */
    @Transactional
    public IntercityTrip createTripByDriver(
            Long driverId,
            Long routeId,
            IntercityVehicleType vehicleType,
            String pickupAddress,
            Double pickupLat,
            Double pickupLng,
            String dropAddress,
            Double dropLat,
            Double dropLng,
            LocalDateTime scheduledDeparture,
            boolean isPrivate,
            BigDecimal totalFare,
            Integer seats,
            Boolean returnTrip,
            LocalDateTime returnTripDeparture,
            Boolean nightFareEnabled,
            BigDecimal nightFareMultiplier,
            BigDecimal distanceKm,
            Boolean premiumNotification
    ) throws ResourceNotFoundException {
        IntercityVehicleConfig config = vehicleConfigRepository.findByVehicleType(vehicleType)
            .orElseThrow(() -> new ResourceNotFoundException("VehicleConfig", "type", vehicleType.name()));
        
        IntercityRoute route = null;
        if (routeId != null) {
            route = routeRepository.findById(routeId).orElse(null);
        }
        
        // Use driver's specified fare, or default from config
        BigDecimal totalPrice = totalFare != null ? totalFare : config.getTotalPrice();
        if (route != null && route.getPriceMultiplier() != null) {
            totalPrice = totalPrice.multiply(route.getPriceMultiplier());
        }
        
        // Apply night fare multiplier if enabled
        if (Boolean.TRUE.equals(nightFareEnabled) && nightFareMultiplier != null) {
            totalPrice = totalPrice.multiply(nightFareMultiplier);
        }
        
        // Generate unique trip code
        String tripCode = generateTripCode();
        
        // Determine total seats
        int totalSeats = seats != null ? seats : config.getMaxSeats();
        if (isPrivate) {
            totalSeats = config.getMaxSeats();
        }
        
        IntercityTrip trip = IntercityTrip.builder()
            .tripCode(tripCode)
            .route(route)
            .vehicleType(vehicleType)
            .vehicleConfig(config)
            .status(IntercityTripStatus.PENDING)
            .totalSeats(totalSeats)
            .seatsBooked(0)
            .minSeats(isPrivate ? totalSeats : config.getMinSeats())
            .totalPrice(totalPrice)
            .currentPerHeadPrice(totalPrice)
            .scheduledDeparture(scheduledDeparture != null ? scheduledDeparture : LocalDateTime.now().plusMinutes(30))
            .pickupAddress(pickupAddress)
            .pickupLatitude(pickupLat)
            .pickupLongitude(pickupLng)
            .dropAddress(dropAddress)
            .dropLatitude(dropLat)
            .dropLongitude(dropLng)
            .isPrivate(isPrivate)
            .returnTrip(returnTrip != null ? returnTrip : false)
            .returnTripDeparture(returnTripDeparture)
            .nightFareEnabled(nightFareEnabled != null ? nightFareEnabled : false)
            .nightFareMultiplier(nightFareMultiplier != null ? nightFareMultiplier : BigDecimal.ONE)
            .distanceKm(distanceKm)
            .premiumNotification(premiumNotification != null ? premiumNotification : false)
            .build();
        
        return tripRepository.save(trip);
    }
    
    /**
     * Find available trips for pooling
     * Returns trips sorted by lowest price (per head) first
     */
    public List<IntercityTrip> findAvailableTripsForPooling(
            IntercityVehicleType vehicleType,
            int seatsNeeded
    ) {
        List<IntercityTripStatus> validStatuses = List.of(
            IntercityTripStatus.PENDING,
            IntercityTripStatus.FILLING,
            IntercityTripStatus.MIN_REACHED
        );
        
        return tripRepository.findAvailableTripsForPooling(validStatuses, vehicleType, LocalDateTime.now())
            .stream()
            .filter(t -> t.getAvailableSeats() >= seatsNeeded)
            .sorted((t1, t2) -> {
                // Sort by current per-head price (lowest first)
                return t1.getCurrentPerHeadPrice().compareTo(t2.getCurrentPerHeadPrice());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find available trips for a route
     */
    public List<IntercityTrip> findAvailableTripsForRoute(Long routeId, int seatsNeeded) {
        List<IntercityTripStatus> validStatuses = List.of(
            IntercityTripStatus.PENDING,
            IntercityTripStatus.FILLING,
            IntercityTripStatus.MIN_REACHED
        );
        
        return tripRepository.findAvailableTripsForRoute(routeId, validStatuses, LocalDateTime.now())
            .stream()
            .filter(t -> t.getAvailableSeats() >= seatsNeeded)
            .collect(Collectors.toList());
    }
    
    /**
     * Add seats to a trip
     */
    @Transactional
    public void addSeatsToTrip(IntercityTrip trip, int seatsToAdd) {
        trip.setSeatsBooked(trip.getSeatsBooked() + seatsToAdd);
        trip.recalculatePerHeadPrice();
        
        // Update status based on seats
        if (trip.getSeatsBooked() >= trip.getMinSeats()) {
            if (trip.getStatus() == IntercityTripStatus.PENDING || 
                trip.getStatus() == IntercityTripStatus.FILLING) {
                trip.setStatus(IntercityTripStatus.MIN_REACHED);
            }
        } else if (trip.getSeatsBooked() > 0) {
            if (trip.getStatus() == IntercityTripStatus.PENDING) {
                trip.setStatus(IntercityTripStatus.FILLING);
                // Start countdown
                trip.setCountdownExpiry(LocalDateTime.now().plusMinutes(DEFAULT_COUNTDOWN_MINUTES));
            }
        }
        
        tripRepository.save(trip);
    }
    
    /**
     * Remove seats from a trip (cancellation)
     */
    @Transactional
    public void removeSeatsFromTrip(IntercityTrip trip, int seatsToRemove) {
        trip.setSeatsBooked(Math.max(0, trip.getSeatsBooked() - seatsToRemove));
        trip.recalculatePerHeadPrice();
        
        // Update status if fell below minimum
        if (trip.getSeatsBooked() < trip.getMinSeats() && 
            trip.getStatus() == IntercityTripStatus.MIN_REACHED) {
            trip.setStatus(IntercityTripStatus.FILLING);
        }
        
        if (trip.getSeatsBooked() == 0) {
            trip.setStatus(IntercityTripStatus.PENDING);
            trip.setCountdownExpiry(null);
        }
        
        tripRepository.save(trip);
    }
    
    /**
     * Get trip by ID
     */
    public IntercityTrip getTripById(Long tripId) throws ResourceNotFoundException {
        return tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
    }
    
    /**
     * Get trip by code
     */
    public IntercityTrip getTripByCode(String tripCode) throws ResourceNotFoundException {
        return tripRepository.findByTripCode(tripCode)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "code", tripCode));
    }
    
    /**
     * Convert trip to DTO
     */
    public IntercityTripDTO toDTO(IntercityTrip trip) {
        Long countdownSeconds = null;
        if (trip.getCountdownExpiry() != null) {
            Duration remaining = Duration.between(LocalDateTime.now(), trip.getCountdownExpiry());
            countdownSeconds = remaining.isNegative() ? 0 : remaining.getSeconds();
        }
        
        BigDecimal projectedPrice = pricingService.calculateProjectedPerHeadPrice(trip, 1);
        String priceMessage = pricingService.generatePriceMessage(trip);
        
        return IntercityTripDTO.builder()
            .tripId(trip.getId())
            .tripCode(trip.getTripCode())
            .vehicleType(trip.getVehicleType())
            .vehicleDisplayName(trip.getVehicleConfig() != null ? trip.getVehicleConfig().getDisplayName() : trip.getVehicleType().name())
            .vehicleImageUrl(trip.getVehicleConfig() != null ? trip.getVehicleConfig().getImageUrl() : null)
            .status(trip.getStatus())
            .pickupAddress(trip.getPickupAddress())
            .pickupLatitude(trip.getPickupLatitude())
            .pickupLongitude(trip.getPickupLongitude())
            .dropAddress(trip.getDropAddress())
            .dropLatitude(trip.getDropLatitude())
            .dropLongitude(trip.getDropLongitude())
            .scheduledDeparture(trip.getScheduledDeparture())
            .countdownExpiry(trip.getCountdownExpiry())
            .countdownSecondsRemaining(countdownSeconds)
            .totalSeats(trip.getTotalSeats())
            .seatsBooked(trip.getSeatsBooked())
            .availableSeats(trip.getAvailableSeats())
            .minSeats(trip.getMinSeats())
            .minSeatsMet(trip.isMinSeatsMet())
            .passengersOnboarded(trip.getPassengersOnboarded() != null ? trip.getPassengersOnboarded() : 0)
            .pendingVerifications(bookingRepository.countPendingVerificationsByTripId(trip.getId()))
            .totalPrice(trip.getTotalPrice())
            .currentPerHeadPrice(trip.getCurrentPerHeadPrice())
            .projectedPriceIfYouJoin(projectedPrice)
            .priceMessage(priceMessage)
            .canJoin(trip.getAvailableSeats() > 0 && !trip.getIsPrivate())
            .build();
    }
    
    /**
     * Dispatch trip (assign driver and start)
     */
    @Transactional
    public void dispatchTrip(Long tripId) throws ResourceNotFoundException {
        IntercityTrip trip = getTripById(tripId);
        
        if (!trip.isMinSeatsMet()) {
            throw new IllegalStateException("Cannot dispatch trip - minimum seats not met");
        }
        
        trip.setStatus(IntercityTripStatus.DISPATCHED);
        tripRepository.save(trip);
        
        log.info("Trip {} dispatched", trip.getTripCode());
        // TODO: Notify driver, notify passengers
    }
    
    /**
     * Start trip
     */
    @Transactional
    public void startTrip(Long tripId) throws ResourceNotFoundException {
        IntercityTrip trip = getTripById(tripId);
        trip.setStatus(IntercityTripStatus.IN_PROGRESS);
        trip.setActualDeparture(LocalDateTime.now());
        tripRepository.save(trip);
    }
    
    /**
     * Complete trip
     */
    @Transactional
    public void completeTrip(Long tripId) throws ResourceNotFoundException {
        IntercityTrip trip = getTripById(tripId);
        trip.setStatus(IntercityTripStatus.COMPLETED);
        trip.setActualArrival(LocalDateTime.now());
        tripRepository.save(trip);
        
        // Handle return trip guarantee if UP trip is filled
        handleReturnTripGuarantee(trip);
    }
    
    /**
     * Return Trip Guarantee: If UP trip is filled, guarantee minimum 2 seats for return trip
     */
    @Transactional
    public void handleReturnTripGuarantee(IntercityTrip upTrip) {
        if (upTrip == null || !Boolean.TRUE.equals(upTrip.getReturnTrip())) {
            return;
        }
        
        // Check if UP trip is completed and filled
        if (upTrip.getStatus() == IntercityTripStatus.COMPLETED && 
            upTrip.getSeatsBooked() >= upTrip.getMinSeats()) {
            
            // Find return trip (same route, opposite direction, same driver)
            List<IntercityTrip> returnTrips = tripRepository.findByRouteIdAndDriverIdAndStatusIn(
                upTrip.getRoute() != null ? upTrip.getRoute().getId() : null,
                upTrip.getDriver() != null ? upTrip.getDriver().getId() : null,
                List.of(IntercityTripStatus.PENDING, IntercityTripStatus.FILLING)
            );
            
            // Find return trip with matching pickup/drop (reversed)
            IntercityTrip returnTrip = returnTrips.stream()
                .filter(rt -> rt.getPickupAddress() != null && 
                             rt.getDropAddress() != null &&
                             rt.getPickupAddress().equals(upTrip.getDropAddress()) &&
                             rt.getDropAddress().equals(upTrip.getPickupAddress()))
                .findFirst()
                .orElse(null);
            
            if (returnTrip != null && returnTrip.getSeatsBooked() < 2) {
                // Guarantee minimum 2 seats for return trip
                if (returnTrip.getMinSeats() < 2) {
                    returnTrip.setMinSeats(2);
                }
                returnTrip.setReturnTripGuarantee(true);
                tripRepository.save(returnTrip);
                
                log.info("Return trip guarantee activated for trip {} - Minimum 2 seats guaranteed", 
                    returnTrip.getTripCode());
            }
        }
    }
    
    /**
     * Cancel trip
     */
    @Transactional
    public void cancelTrip(Long tripId, String reason) throws ResourceNotFoundException {
        IntercityTrip trip = getTripById(tripId);
        trip.setStatus(IntercityTripStatus.CANCELLED);
        tripRepository.save(trip);
        
        log.info("Trip {} cancelled: {}", trip.getTripCode(), reason);
        // TODO: Refund all bookings
    }
    
    /**
     * Scheduled job to process expired countdowns
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void processExpiredCountdowns() {
        List<IntercityTrip> expiredTrips = tripRepository.findTripsWithExpiredCountdown(
            IntercityTripStatus.FILLING, LocalDateTime.now()
        );
        
        for (IntercityTrip trip : expiredTrips) {
            log.info("Processing expired countdown for trip {}", trip.getTripCode());
            
            if (trip.isMinSeatsMet()) {
                // Minimum met - dispatch
                trip.setStatus(IntercityTripStatus.MIN_REACHED);
                tripRepository.save(trip);
                // TODO: Auto-dispatch or notify admin
            } else {
                // Minimum not met - mark expired and notify passengers
                trip.setStatus(IntercityTripStatus.EXPIRED);
                tripRepository.save(trip);
                // TODO: Notify passengers about alternatives
            }
        }
    }
    
    // ============== Private Helper Methods ==============
    
    private String generateTripCode() {
        String code;
        do {
            code = "T" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        } while (tripRepository.existsByTripCode(code));
        return code;
    }
}

