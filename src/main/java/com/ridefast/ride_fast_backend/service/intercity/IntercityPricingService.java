package com.ridefast.ride_fast_backend.service.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.IntercityAlternativeDTO;
import com.ridefast.ride_fast_backend.dto.intercity.IntercityVehicleOptionDTO;
import com.ridefast.ride_fast_backend.dto.intercity.IntercityVehicleSearchRequest;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.model.intercity.IntercityRoute;
import com.ridefast.ride_fast_backend.model.intercity.IntercityTrip;
import com.ridefast.ride_fast_backend.model.intercity.IntercityVehicleConfig;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityRouteRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityTripRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityVehicleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.ridefast.ride_fast_backend.enums.IntercityTripStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for intercity pricing calculations
 * Handles dynamic per-head pricing based on seats filled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntercityPricingService {
    
    private final IntercityVehicleConfigRepository vehicleConfigRepository;
    private final IntercityRouteRepository routeRepository;
    private final IntercityTripRepository tripRepository;
    
    /**
     * Search vehicle options based on search criteria
     */
    public List<IntercityVehicleOptionDTO> searchVehicleOptions(IntercityVehicleSearchRequest request) {
        IntercityRoute route = null;
        
        // Find route by ID if provided
        if (request.getRouteId() != null && request.getRouteId() > 0) {
            route = routeRepository.findById(request.getRouteId()).orElse(null);
        }
        
        // Get base vehicle options
        List<IntercityVehicleOptionDTO> options = getVehicleOptions(route);
        
        // Filter by vehicle type if specified
        if (request.getVehicleType() != null) {
            options = options.stream()
                .filter(opt -> opt.getVehicleType() == request.getVehicleType())
                .collect(Collectors.toList());
        }
        
        // Filter by seats needed
        if (request.getSeatsNeeded() != null && request.getSeatsNeeded() > 0) {
            options = options.stream()
                .filter(opt -> opt.getAvailableSeats() >= request.getSeatsNeeded())
                .collect(Collectors.toList());
        }
        
        // Enrich with route information
        if (route != null) {
            final IntercityRoute finalRoute = route;
            options.forEach(opt -> {
                opt.setRouteId(finalRoute.getId());
                opt.setDistanceKm(finalRoute.getDistanceKm());
                opt.setEstimatedDurationMinutes(finalRoute.getEstimatedDurationMinutes());
            });
        }
        
        // Set estimated departure time
        LocalDateTime departure = request.getPreferredDeparture() != null 
            ? request.getPreferredDeparture() 
            : LocalDateTime.now().plusHours(1);
        options.forEach(opt -> opt.setEstimatedDeparture(departure));
        
        // Check for existing trips with available seats
        enrichWithExistingTrips(options, request);
        
        return options;
    }
    
    /**
     * Enrich vehicle options with existing trip seat availability
     */
    private void enrichWithExistingTrips(List<IntercityVehicleOptionDTO> options, IntercityVehicleSearchRequest request) {
        List<IntercityTripStatus> poolingStatuses = Arrays.asList(
            IntercityTripStatus.FILLING,
            IntercityTripStatus.MIN_REACHED,
            IntercityTripStatus.PENDING
        );
        
        for (IntercityVehicleOptionDTO option : options) {
            // Find existing trips for this vehicle type that have seats
            List<IntercityTrip> existingTrips = tripRepository.findAvailableTripsForPooling(
                poolingStatuses,
                option.getVehicleType(),
                LocalDateTime.now()
            );
            
            if (!existingTrips.isEmpty()) {
                // Use the trip with most seats booked (likely to depart sooner)
                IntercityTrip bestTrip = existingTrips.stream()
                    .max(Comparator.comparingInt(IntercityTrip::getSeatsBooked))
                    .orElse(existingTrips.get(0));
                
                option.setSeatsBooked(bestTrip.getSeatsBooked());
                option.setAvailableSeats(bestTrip.getAvailableSeats());
                option.setSeatsRemaining(bestTrip.getAvailableSeats());
                option.setSeatsTotal(bestTrip.getTotalSeats());
                option.setCurrentPerHeadPrice(calculatePerHeadPrice(bestTrip));
                option.setEstimatedDeparture(bestTrip.getScheduledDeparture());
                
                // Calculate wait time
                if (bestTrip.getScheduledDeparture() != null) {
                    long minutes = java.time.Duration.between(LocalDateTime.now(), bestTrip.getScheduledDeparture()).toMinutes();
                    option.setEstimatedWaitMinutes((int) Math.max(0, minutes));
                }
            } else {
                // No existing trips, set default values
                option.setSeatsBooked(0);
                option.setSeatsRemaining(option.getMaxSeats());
                option.setSeatsTotal(option.getMaxSeats());
                option.setEstimatedWaitMinutes(15); // Default wait time
            }
        }
    }
    
    /**
     * Get all vehicle options with pricing
     */
    public List<IntercityVehicleOptionDTO> getVehicleOptions(IntercityRoute route) {
        List<IntercityVehicleConfig> configs = vehicleConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<IntercityVehicleOptionDTO> options = new ArrayList<>();
        
        BigDecimal priceMultiplier = route != null ? route.getPriceMultiplier() : BigDecimal.ONE;
        
        for (IntercityVehicleConfig config : configs) {
            BigDecimal adjustedPrice = config.getTotalPrice().multiply(priceMultiplier)
                .setScale(2, RoundingMode.CEILING);
            
            IntercityVehicleOptionDTO option = IntercityVehicleOptionDTO.builder()
                .vehicleType(config.getVehicleType())
                .displayName(config.getDisplayName())
                .description(config.getDescription())
                .imageUrl(config.getImageUrl())
                .totalPrice(adjustedPrice)
                .maxSeats(config.getMaxSeats())
                .minSeats(config.getMinSeats())
                .currentPerHeadPrice(adjustedPrice) // Full price initially
                .availableSeats(config.getMaxSeats())
                .seatsBooked(0)
                .seatsRemaining(config.getMaxSeats())
                .seatsTotal(config.getMaxSeats())
                .targetCustomer(config.getTargetCustomer())
                .recommendationTag(config.getRecommendationTag())
                .isRecommended(false)
                .routeId(route != null ? route.getId() : null)
                .distanceKm(route != null ? route.getDistanceKm() : null)
                .estimatedDurationMinutes(route != null ? route.getEstimatedDurationMinutes() : null)
                .build();
            
            options.add(option);
        }
        
        // Mark best value as recommended
        markRecommendedOption(options);
        
        return options;
    }
    
    /**
     * Calculate per-head price for a trip
     */
    public BigDecimal calculatePerHeadPrice(IntercityTrip trip) {
        if (trip.getSeatsBooked() <= 0) {
            return trip.getTotalPrice();
        }
        return trip.getTotalPrice()
            .divide(BigDecimal.valueOf(trip.getSeatsBooked()), 2, RoundingMode.CEILING);
    }
    
    /**
     * Calculate per-head price if a new booking joins
     */
    public BigDecimal calculateProjectedPerHeadPrice(IntercityTrip trip, int additionalSeats) {
        int projectedSeats = trip.getSeatsBooked() + additionalSeats;
        if (projectedSeats <= 0) {
            return trip.getTotalPrice();
        }
        return trip.getTotalPrice()
            .divide(BigDecimal.valueOf(projectedSeats), 2, RoundingMode.CEILING);
    }
    
    /**
     * Generate price message for customers
     */
    public String generatePriceMessage(IntercityTrip trip) {
        int available = trip.getAvailableSeats();
        int booked = trip.getSeatsBooked();
        BigDecimal currentPrice = calculatePerHeadPrice(trip);
        
        if (available == 0) {
            return "Trip is full";
        }
        
        if (booked < trip.getMinSeats()) {
            int needed = trip.getMinSeats() - booked;
            return String.format("%d more seat(s) needed to confirm trip", needed);
        }
        
        if (available == 1) {
            BigDecimal projectedPrice = calculateProjectedPerHeadPrice(trip, 1);
            return String.format("1 seat available → fare may reduce to ₹%.0f if filled", projectedPrice);
        }
        
        return String.format("%d seats available → fare: ₹%.0f/seat", available, currentPrice);
    }
    
    /**
     * Get alternative vehicle suggestions when min seats not met
     */
    public List<IntercityAlternativeDTO> getAlternatives(
            IntercityVehicleType currentType,
            BigDecimal routePriceMultiplier
    ) {
        List<IntercityVehicleConfig> configs = vehicleConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<IntercityAlternativeDTO> alternatives = new ArrayList<>();
        
        BigDecimal multiplier = routePriceMultiplier != null ? routePriceMultiplier : BigDecimal.ONE;
        
        for (IntercityVehicleConfig config : configs) {
            if (config.getVehicleType() == currentType) continue; // Skip current type
            
            BigDecimal adjustedPrice = config.getTotalPrice().multiply(multiplier)
                .setScale(2, RoundingMode.CEILING);
            
            // Calculate best case per-seat price (if all seats filled)
            BigDecimal bestPerSeat = adjustedPrice.divide(
                BigDecimal.valueOf(config.getMaxSeats()), 2, RoundingMode.CEILING
            );
            
            String reason = determineAlternativeReason(config, currentType);
            
            IntercityAlternativeDTO alt = IntercityAlternativeDTO.builder()
                .vehicleType(config.getVehicleType())
                .displayName(config.getDisplayName())
                .imageUrl(config.getImageUrl())
                .price(adjustedPrice)
                .perSeatPrice(bestPerSeat)
                .reason(reason)
                .availableTrips(0) // To be filled by caller
                .estimatedWaitMinutes(5) // Default
                .isRecommended(isBetterValue(config, currentType))
                .build();
            
            alternatives.add(alt);
        }
        
        // Sort by value (per-seat price)
        alternatives.sort(Comparator.comparing(IntercityAlternativeDTO::getPerSeatPrice));
        
        return alternatives;
    }
    
    /**
     * Get vehicle config by type
     */
    public Optional<IntercityVehicleConfig> getVehicleConfig(IntercityVehicleType vehicleType) {
        return vehicleConfigRepository.findByVehicleType(vehicleType);
    }
    
    /**
     * Calculate total booking amount
     */
    public BigDecimal calculateBookingAmount(IntercityTrip trip, int seatsToBook, boolean isPrivate) {
        if (isPrivate) {
            return trip.getTotalPrice();
        }
        
        // For pool booking, calculate based on projected seats
        BigDecimal perHeadPrice = calculateProjectedPerHeadPrice(trip, seatsToBook);
        return perHeadPrice.multiply(BigDecimal.valueOf(seatsToBook));
    }
    
    // ============== Private Helper Methods ==============
    
    private void markRecommendedOption(List<IntercityVehicleOptionDTO> options) {
        // Find best value (lowest per-seat price at max capacity)
        IntercityVehicleOptionDTO bestValue = null;
        BigDecimal lowestPerSeat = null;
        
        for (IntercityVehicleOptionDTO opt : options) {
            BigDecimal perSeat = opt.getTotalPrice().divide(
                BigDecimal.valueOf(opt.getMaxSeats()), 2, RoundingMode.CEILING
            );
            if (lowestPerSeat == null || perSeat.compareTo(lowestPerSeat) < 0) {
                lowestPerSeat = perSeat;
                bestValue = opt;
            }
        }
        
        if (bestValue != null) {
            bestValue.setIsRecommended(true);
            if (bestValue.getRecommendationTag() == null) {
                bestValue.setRecommendationTag("Best Value");
            }
        }
    }
    
    private String determineAlternativeReason(IntercityVehicleConfig config, IntercityVehicleType currentType) {
        switch (config.getVehicleType()) {
            case TATA_MAGIC_LITE:
                return "Best value per head, fills quickly";
            case AUTO_NORMAL:
                return "Cheaper than car, fast dispatch";
            case CAR_NORMAL:
                return "Good comfort, value for money";
            case CAR_PREMIUM_EXPRESS:
                return "Premium comfort, exclusive ride";
            default:
                return "Alternative option available";
        }
    }
    
    private boolean isBetterValue(IntercityVehicleConfig config, IntercityVehicleType currentType) {
        // Tata Magic Lite is generally best value
        if (config.getVehicleType() == IntercityVehicleType.TATA_MAGIC_LITE) {
            return true;
        }
        // Auto is better value than cars
        if (config.getVehicleType() == IntercityVehicleType.AUTO_NORMAL &&
            (currentType == IntercityVehicleType.CAR_NORMAL || currentType == IntercityVehicleType.CAR_PREMIUM_EXPRESS)) {
            return true;
        }
        return false;
    }
}

