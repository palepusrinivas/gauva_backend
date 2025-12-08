package com.ridefast.ride_fast_backend.controller.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.DriverTripPublishRequest;
import com.ridefast.ride_fast_backend.dto.intercity.IntercityOtpVerifyRequest;
import com.ridefast.ride_fast_backend.dto.intercity.IntercityOtpVerifyResponse;
import com.ridefast.ride_fast_backend.dto.intercity.IntercityTripDTO;
import com.ridefast.ride_fast_backend.enums.IntercityBookingType;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.intercity.IntercityBooking;
import com.ridefast.ride_fast_backend.model.intercity.IntercityTrip;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityBookingRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityTripRepository;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.intercity.IntercityTripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Driver-facing API for intercity trip management
 */
@RestController
@RequestMapping("/api/driver/intercity")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
@Slf4j
public class DriverIntercityController {

    private final IntercityTripRepository tripRepository;
    private final IntercityBookingRepository bookingRepository;
    private final IntercityTripService tripService;
    private final DriverService driverService;
    private final com.ridefast.ride_fast_backend.service.WalletService walletService;

    /**
     * Driver publishes a new intercity trip
     * 
     * POST /api/driver/intercity/publish
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishTrip(
            @RequestHeader("Authorization") String jwtToken,
            @RequestBody DriverTripPublishRequest request
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        
        // Check minimum wallet balance (₹200)
        try {
            java.math.BigDecimal currentBalance = walletService.getBalance(
                com.ridefast.ride_fast_backend.enums.WalletOwnerType.DRIVER,
                driver.getId().toString()
            );
            
            java.math.BigDecimal minBalance = new java.math.BigDecimal("200");
            if (currentBalance.compareTo(minBalance) < 0) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of(
                        "error", "Insufficient wallet balance",
                        "message", String.format("Driver must maintain minimum ₹200 balance. Current balance: ₹%s", currentBalance),
                        "currentBalance", currentBalance,
                        "minimumRequired", minBalance
                    ));
            }
        } catch (Exception e) {
            log.warn("Could not check wallet balance for driver {}: {}", driver.getId(), e.getMessage());
            // Continue with trip creation if balance check fails
        }
        
        boolean isPrivate = request.getBookingType() == IntercityBookingType.PRIVATE;
        
        // Create trip with driver's details
        IntercityTrip trip = tripService.createTripByDriver(
                driver.getId(),
                request.getRouteId(),
                request.getVehicleType(),
                request.getPickupAddress(),
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropAddress(),
                request.getDropLatitude(),
                request.getDropLongitude(),
                request.getScheduledDeparture(),
                isPrivate,
                request.getTotalFare(),
                request.getSeats(),
                request.getReturnTrip(),
                request.getReturnTripDeparture(),
                request.getNightFareEnabled(),
                request.getNightFareMultiplier(),
                request.getDistanceKm(),
                request.getPremiumNotification()
        );
        
        log.info("Driver {} published trip {} - Type: {}, Vehicle: {}", 
                driver.getId(), trip.getTripCode(), request.getBookingType(), request.getVehicleType());
        
        return ResponseEntity.ok(tripService.toDTO(trip));
    }

    /**
     * Get driver's assigned intercity trips
     * 
     * GET /api/driver/intercity/trips
     */
    @GetMapping("/trips")
    public ResponseEntity<List<IntercityTripDTO>> getMyTrips(
            @RequestHeader("Authorization") String jwtToken
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        List<IntercityTrip> trips = tripRepository.findByDriverIdAndStatusIn(
                driver.getId(),
                List.of(
                        com.ridefast.ride_fast_backend.enums.IntercityTripStatus.DISPATCHED,
                        com.ridefast.ride_fast_backend.enums.IntercityTripStatus.IN_PROGRESS
                )
        );
        List<IntercityTripDTO> dtos = trips.stream().map(tripService::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get trip details with all bookings
     * 
     * GET /api/driver/intercity/trips/{tripId}
     */
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<IntercityTripDTO> getTripDetails(
            @RequestHeader("Authorization") String jwtToken,
            @PathVariable Long tripId
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Verify driver is assigned to this trip
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip", "id", tripId);
        }
        
        return ResponseEntity.ok(tripService.toDTO(trip));
    }

    /**
     * Get all bookings for a trip with OTP status
     * 
     * GET /api/driver/intercity/trips/{tripId}/bookings
     */
    @GetMapping("/trips/{tripId}/bookings")
    public ResponseEntity<List<Map<String, Object>>> getTripBookings(
            @RequestHeader("Authorization") String jwtToken,
            @PathVariable Long tripId
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Verify driver is assigned to this trip
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip", "id", tripId);
        }
        
        List<IntercityBooking> bookings = bookingRepository.findByTripIdAndStatusIn(
                tripId,
                List.of(
                        com.ridefast.ride_fast_backend.enums.IntercityBookingStatus.CONFIRMED,
                        com.ridefast.ride_fast_backend.enums.IntercityBookingStatus.COMPLETED
                )
        );
        
        List<Map<String, Object>> bookingList = bookings.stream().map(booking -> Map.<String, Object>of(
                "bookingId", booking.getId(),
                "bookingCode", booking.getBookingCode(),
                "passengerName", booking.getUser().getFullName() != null ? booking.getUser().getFullName() : "N/A",
                "passengerPhone", booking.getContactPhone() != null ? booking.getContactPhone() : booking.getUser().getPhone(),
                "seatsBooked", booking.getSeatsBooked(),
                "otpVerified", booking.getOtpVerified() != null ? booking.getOtpVerified() : false,
                "passengersOnboarded", booking.getPassengersOnboarded() != null ? booking.getPassengersOnboarded() : 0,
                "otpVerifiedAt", booking.getOtpVerifiedAt() != null ? booking.getOtpVerifiedAt().toString() : null
        )).toList();
        
        return ResponseEntity.ok(bookingList);
    }

    /**
     * Verify OTP and onboard passengers
     * 
     * POST /api/driver/intercity/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<IntercityOtpVerifyResponse> verifyOtp(
            @RequestHeader("Authorization") String jwtToken,
            @RequestBody IntercityOtpVerifyRequest request
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        
        // Find the booking
        IntercityBooking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));
        
        IntercityTrip trip = booking.getTrip();
        
        // Verify driver is assigned to this trip
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            return ResponseEntity.badRequest().body(
                    IntercityOtpVerifyResponse.builder()
                            .success(false)
                            .message("You are not assigned to this trip")
                            .build()
            );
        }
        
        // Check if already verified
        if (Boolean.TRUE.equals(booking.getOtpVerified())) {
            return ResponseEntity.ok(
                    IntercityOtpVerifyResponse.builder()
                            .success(true)
                            .message("Already verified")
                            .bookingId(booking.getId())
                            .bookingCode(booking.getBookingCode())
                            .passengerName(booking.getUser().getFullName())
                            .passengerPhone(booking.getContactPhone())
                            .passengersVerified(booking.getPassengersOnboarded())
                            .verifiedAt(booking.getOtpVerifiedAt())
                            .tripId(trip.getId())
                            .totalSeatsBooked(trip.getSeatsBooked())
                            .totalPassengersOnboarded(trip.getPassengersOnboarded())
                            .pendingVerifications(countPendingVerifications(trip.getId()))
                            .build()
            );
        }
        
        // Verify OTP
        if (!booking.getOtp().equals(request.getOtp())) {
            return ResponseEntity.badRequest().body(
                    IntercityOtpVerifyResponse.builder()
                            .success(false)
                            .message("Invalid OTP")
                            .bookingId(booking.getId())
                            .build()
            );
        }
        
        // Determine passengers boarding
        int passengersBoarding = request.getPassengersBoarding() != null 
                ? request.getPassengersBoarding() 
                : booking.getSeatsBooked();
        
        // Validate passengers count
        if (passengersBoarding > booking.getSeatsBooked()) {
            passengersBoarding = booking.getSeatsBooked();
        }
        if (passengersBoarding < 0) {
            passengersBoarding = 0;
        }
        
        // Update booking
        booking.setOtpVerified(true);
        booking.setOtpVerifiedAt(LocalDateTime.now());
        booking.setPassengersOnboarded(passengersBoarding);
        bookingRepository.save(booking);
        
        // Update trip onboarded count
        int totalOnboarded = calculateTotalOnboarded(trip.getId());
        trip.setPassengersOnboarded(totalOnboarded);
        tripRepository.save(trip);
        
        log.info("OTP verified for booking {} on trip {}. Passengers onboarded: {}", 
                booking.getBookingCode(), trip.getTripCode(), passengersBoarding);
        
        return ResponseEntity.ok(
                IntercityOtpVerifyResponse.builder()
                        .success(true)
                        .message("OTP verified successfully")
                        .bookingId(booking.getId())
                        .bookingCode(booking.getBookingCode())
                        .passengerName(booking.getUser().getFullName())
                        .passengerPhone(booking.getContactPhone())
                        .passengersVerified(passengersBoarding)
                        .verifiedAt(booking.getOtpVerifiedAt())
                        .tripId(trip.getId())
                        .totalSeatsBooked(trip.getSeatsBooked())
                        .totalPassengersOnboarded(totalOnboarded)
                        .pendingVerifications(countPendingVerifications(trip.getId()))
                        .build()
        );
    }

    /**
     * Get onboarding summary for a trip
     * 
     * GET /api/driver/intercity/trips/{tripId}/onboarding-summary
     */
    @GetMapping("/trips/{tripId}/onboarding-summary")
    public ResponseEntity<Map<String, Object>> getOnboardingSummary(
            @RequestHeader("Authorization") String jwtToken,
            @PathVariable Long tripId
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Verify driver is assigned to this trip
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip", "id", tripId);
        }
        
        int totalOnboarded = calculateTotalOnboarded(tripId);
        int pendingVerifications = countPendingVerifications(tripId);
        int totalBookings = countTotalBookings(tripId);
        int verifiedBookings = countVerifiedBookings(tripId);
        
        return ResponseEntity.ok(Map.of(
                "tripId", trip.getId(),
                "tripCode", trip.getTripCode(),
                "totalSeats", trip.getTotalSeats(),
                "seatsBooked", trip.getSeatsBooked(),
                "passengersOnboarded", totalOnboarded,
                "totalBookings", totalBookings,
                "verifiedBookings", verifiedBookings,
                "pendingVerifications", pendingVerifications,
                "allVerified", pendingVerifications == 0
        ));
    }

    /**
     * Start trip (after verifying minimum passengers)
     * 
     * POST /api/driver/intercity/trips/{tripId}/start
     */
    @PostMapping("/trips/{tripId}/start")
    public ResponseEntity<IntercityTripDTO> startTrip(
            @RequestHeader("Authorization") String jwtToken,
            @PathVariable Long tripId
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Verify driver is assigned to this trip
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip", "id", tripId);
        }
        
        // Check if trip can be started
        if (trip.getStatus() != com.ridefast.ride_fast_backend.enums.IntercityTripStatus.DISPATCHED) {
            throw new IllegalStateException("Trip cannot be started from current status: " + trip.getStatus());
        }
        
        // Update trip status
        trip.setStatus(com.ridefast.ride_fast_backend.enums.IntercityTripStatus.IN_PROGRESS);
        trip.setActualDeparture(LocalDateTime.now());
        tripRepository.save(trip);
        
        log.info("Trip {} started by driver {}. Passengers onboarded: {}/{}", 
                trip.getTripCode(), driver.getId(), trip.getPassengersOnboarded(), trip.getSeatsBooked());
        
        return ResponseEntity.ok(tripService.toDTO(trip));
    }

    /**
     * Complete trip
     * 
     * POST /api/driver/intercity/trips/{tripId}/complete
     */
    @PostMapping("/trips/{tripId}/complete")
    public ResponseEntity<IntercityTripDTO> completeTrip(
            @RequestHeader("Authorization") String jwtToken,
            @PathVariable Long tripId
    ) throws ResourceNotFoundException {
        Driver driver = driverService.getRequestedDriverProfile(jwtToken);
        IntercityTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Verify driver is assigned to this trip
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip", "id", tripId);
        }
        
        // Check if trip can be completed
        if (trip.getStatus() != com.ridefast.ride_fast_backend.enums.IntercityTripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Trip cannot be completed from current status: " + trip.getStatus());
        }
        
        // Update trip status
        trip.setStatus(com.ridefast.ride_fast_backend.enums.IntercityTripStatus.COMPLETED);
        trip.setActualArrival(LocalDateTime.now());
        tripRepository.save(trip);
        
        // Update all confirmed bookings to completed
        List<IntercityBooking> bookings = bookingRepository.findByTripIdAndStatusIn(
                tripId,
                List.of(com.ridefast.ride_fast_backend.enums.IntercityBookingStatus.CONFIRMED)
        );
        for (IntercityBooking booking : bookings) {
            booking.setStatus(com.ridefast.ride_fast_backend.enums.IntercityBookingStatus.COMPLETED);
            bookingRepository.save(booking);
        }
        
        log.info("Trip {} completed by driver {}. Total passengers onboarded: {}", 
                trip.getTripCode(), driver.getId(), trip.getPassengersOnboarded());
        
        return ResponseEntity.ok(tripService.toDTO(trip));
    }

    // Helper methods
    private int calculateTotalOnboarded(Long tripId) {
        return bookingRepository.sumPassengersOnboardedByTripId(tripId);
    }

    private int countPendingVerifications(Long tripId) {
        return bookingRepository.countPendingVerificationsByTripId(tripId);
    }

    private int countTotalBookings(Long tripId) {
        return bookingRepository.countByTripIdAndStatusIn(
                tripId,
                List.of(
                        com.ridefast.ride_fast_backend.enums.IntercityBookingStatus.CONFIRMED,
                        com.ridefast.ride_fast_backend.enums.IntercityBookingStatus.COMPLETED
                )
        );
    }

    private int countVerifiedBookings(Long tripId) {
        return bookingRepository.countVerifiedByTripId(tripId);
    }
}

