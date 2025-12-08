package com.ridefast.ride_fast_backend.service.intercity;

import com.ridefast.ride_fast_backend.dto.intercity.*;
import com.ridefast.ride_fast_backend.enums.*;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.intercity.*;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.intercity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main service for intercity bookings
 * Handles the complete booking flow for both private and share pool bookings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntercityBookingService {
    
    private final IntercityBookingRepository bookingRepository;
    private final IntercitySeatBookingRepository seatBookingRepository;
    private final IntercityTripRepository tripRepository;
    private final IntercityRouteRepository routeRepository;
    private final IntercityVehicleConfigRepository vehicleConfigRepository;
    private final UserRepository userRepository;
    
    private final IntercityTripService tripService;
    private final IntercityPricingService pricingService;
    private final com.ridefast.ride_fast_backend.service.WalletService walletService;
    
    /** Lock expiry time in minutes */
    private static final int SEAT_LOCK_MINUTES = 10;
    
    /** Payment timeout in minutes */
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;
    
    /** Commission rate: 5% */
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.05");
    
    /** Minimum driver wallet balance: ₹200 */
    private static final BigDecimal MIN_DRIVER_BALANCE = new BigDecimal("200");
    
    /**
     * Search for available trips and vehicle options
     */
    public IntercityTripSearchResponse searchTrips(IntercityTripSearchRequest request) {
        // Find nearby route if exists
        IntercityRoute route = null;
        if (request.getRouteId() != null) {
            route = routeRepository.findById(request.getRouteId()).orElse(null);
        } else {
            // Try to find matching route
            List<IntercityRoute> routes = routeRepository.findNearbyRoutes(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropLatitude(),
                request.getDropLongitude(),
                request.getSearchRadiusKm()
            );
            if (!routes.isEmpty()) {
                route = routes.get(0);
            }
        }
        
        // Get vehicle options
        List<IntercityVehicleOptionDTO> vehicleOptions = pricingService.getVehicleOptions(route);
        
        // Find existing trips that can be joined
        List<IntercityTripDTO> availableTrips = new ArrayList<>();
        if (request.getVehicleType() != null) {
            List<IntercityTrip> trips = tripService.findAvailableTripsForPooling(
                request.getVehicleType(), 
                request.getSeatsNeeded()
            );
            availableTrips = trips.stream()
                .map(tripService::toDTO)
                .collect(Collectors.toList());
        }
        
        // Determine recommendation
        String recommendedVehicle = "TATA_MAGIC_LITE";
        String recommendationReason = "Best value per head, fills quickly";
        
        IntercityTripSearchResponse.RouteInfo routeInfo = null;
        if (route != null) {
            routeInfo = IntercityTripSearchResponse.RouteInfo.builder()
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .originName(route.getOriginName())
                .destinationName(route.getDestinationName())
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getDurationMinutes())
                .build();
        }
        
        return IntercityTripSearchResponse.builder()
            .vehicleOptions(vehicleOptions)
            .availableTrips(availableTrips)
            .route(routeInfo)
            .recommendedVehicle(recommendedVehicle)
            .recommendationReason(recommendationReason)
            .build();
    }
    
    /**
     * Create a new booking
     */
    @Transactional
    public IntercityBookingResponse createBooking(String userId, IntercityBookingRequest request) throws ResourceNotFoundException {
        MyUser user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        IntercityVehicleConfig vehicleConfig = vehicleConfigRepository.findByVehicleType(request.getVehicleType())
            .orElseThrow(() -> new ResourceNotFoundException("VehicleConfig", "type", request.getVehicleType().name()));
        
        IntercityTrip trip;
        int seatsToBook;
        boolean isPrivate = request.getBookingType() == IntercityBookingType.PRIVATE;
        
        if (isPrivate) {
            // Private booking - create new trip with all seats
            seatsToBook = vehicleConfig.getMaxSeats();
            trip = tripService.createTrip(
                request.getRouteId(),
                request.getVehicleType(),
                request.getPickupAddress(),
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropAddress(),
                request.getDropLatitude(),
                request.getDropLongitude(),
                request.getScheduledDeparture(),
                true
            );
        } else {
            // Share pool booking
            seatsToBook = request.getSeatsToBook() != null ? request.getSeatsToBook() : 1;
            
            if (request.getTripId() != null) {
                // Join existing trip
                trip = tripService.getTripById(request.getTripId());
                if (trip.getAvailableSeats() < seatsToBook) {
                    throw new IllegalStateException("Not enough seats available");
                }
            } else {
                // Create new trip
                trip = tripService.createTrip(
                    request.getRouteId(),
                    request.getVehicleType(),
                    request.getPickupAddress(),
                    request.getPickupLatitude(),
                    request.getPickupLongitude(),
                    request.getDropAddress(),
                    request.getDropLatitude(),
                    request.getDropLongitude(),
                    request.getScheduledDeparture(),
                    false
                );
            }
        }
        
        // Calculate pricing
        BigDecimal perSeatAmount = isPrivate ? 
            trip.getTotalPrice() : 
            pricingService.calculateProjectedPerHeadPrice(trip, seatsToBook);
        BigDecimal totalAmount = isPrivate ? 
            trip.getTotalPrice() : 
            perSeatAmount.multiply(BigDecimal.valueOf(seatsToBook));
        
        // Create booking - Set to HOLD status to prevent direct driver contact
        String bookingCode = generateBookingCode();
        IntercityBooking booking = IntercityBooking.builder()
            .bookingCode(bookingCode)
            .user(user)
            .trip(trip)
            .bookingType(request.getBookingType())
            .status(IntercityBookingStatus.HOLD)  // Set to HOLD - admin must confirm
            .seatsBooked(seatsToBook)
            .totalAmount(totalAmount)
            .perSeatAmount(perSeatAmount)
            .paymentStatus(PaymentStatus.PENDING)
            .contactPhone(request.getContactPhone())
            .specialInstructions(request.getSpecialInstructions())
            .build();
        
        booking = bookingRepository.save(booking);
        
        // Create seat bookings
        List<IntercitySeatBooking> seatBookings = new ArrayList<>();
        for (int i = 0; i < seatsToBook; i++) {
            Integer seatNumber = seatBookingRepository.findNextSeatNumber(trip.getId());
            
            String passengerName = null;
            String passengerPhone = null;
            if (request.getPassengers() != null && i < request.getPassengers().size()) {
                passengerName = request.getPassengers().get(i).getName();
                passengerPhone = request.getPassengers().get(i).getPhone();
            }
            
            // Get passenger name and phone with fallbacks
            String finalPassengerName = passengerName != null ? passengerName : 
                (user.getFullName() != null ? user.getFullName() : 
                 (user.getFirstName() != null ? user.getFirstName() : "Passenger"));
            String finalPassengerPhone = passengerPhone != null ? passengerPhone : 
                (user.getPhone() != null ? user.getPhone() : "");
            
            IntercitySeatBooking seatBooking = IntercitySeatBooking.builder()
                .trip(trip)
                .booking(booking)
                .user(user)
                .seatNumber(seatNumber)
                .status(IntercitySeatStatus.LOCKED)
                .pricePaid(perSeatAmount)
                .passengerName(finalPassengerName)
                .passengerPhone(finalPassengerPhone)
                .lockExpiry(LocalDateTime.now().plusMinutes(SEAT_LOCK_MINUTES))
                .build();
            
            seatBookings.add(seatBookingRepository.save(seatBooking));
        }
        
        booking.setSeatBookings(seatBookings);
        
        // Update trip seats (locked seats count towards booked)
        tripService.addSeatsToTrip(trip, seatsToBook);
        
        log.info("Created booking {} for user {} - {} seats on trip {}", 
            bookingCode, userId, seatsToBook, trip.getTripCode());
        
        return toResponse(booking);
    }
    
    /**
     * Confirm booking after payment
     */
    @Transactional
    public IntercityBookingResponse confirmBooking(Long bookingId, String razorpayPaymentId, IntercityPaymentMethod paymentMethod) throws ResourceNotFoundException {
        IntercityBooking booking = getBookingById(bookingId);
        
        if (booking.getStatus() != IntercityBookingStatus.PENDING && 
            booking.getStatus() != IntercityBookingStatus.HOLD) {
            throw new IllegalStateException("Booking is not in pending or hold state");
        }
        
        // Calculate commission (5% of total amount)
        BigDecimal commissionAmount = booking.getTotalAmount().multiply(COMMISSION_RATE);
        
        // Update booking
        booking.setStatus(IntercityBookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setRazorpayPaymentId(razorpayPaymentId);
        booking.setPaymentMethod(paymentMethod);
        booking.setCommissionAmount(commissionAmount);
        booking.setConfirmedAt(LocalDateTime.now());
        
        // Confirm all seat bookings
        for (IntercitySeatBooking seat : booking.getSeatBookings()) {
            seat.setStatus(IntercitySeatStatus.BOOKED);
            seat.setLockExpiry(null);
        }
        
        // Deduct commission based on payment method
        IntercityTrip trip = booking.getTrip();
        if (trip.getDriver() != null) {
            if (paymentMethod == IntercityPaymentMethod.UPI || 
                paymentMethod == IntercityPaymentMethod.WALLET ||
                paymentMethod == IntercityPaymentMethod.ONLINE) {
                // Instant commission deduction for UPI/Wallet/Online
                try {
                    walletService.debit(
                        com.ridefast.ride_fast_backend.enums.WalletOwnerType.DRIVER,
                        trip.getDriver().getId().toString(),
                        commissionAmount,
                        "INTERCITY_COMMISSION",
                        booking.getId().toString(),
                        String.format("5%% commission for intercity booking %s", booking.getBookingCode())
                    );
                    booking.setCommissionDeducted(true);
                    booking.setCommissionDeductedAt(LocalDateTime.now());
                    log.info("Instant commission deducted: ₹{} for booking {} (Payment: {})", 
                        commissionAmount, booking.getBookingCode(), paymentMethod);
                } catch (IllegalArgumentException e) {
                    log.error("Failed to deduct commission for booking {}: {}", 
                        booking.getBookingCode(), e.getMessage());
                    // Continue with booking confirmation even if commission deduction fails
                    // Admin can manually deduct later
                }
            } else if (paymentMethod == IntercityPaymentMethod.CASH) {
                // Cash payment - commission will be deducted at end of day
                booking.setCommissionDeducted(false);
                log.info("Cash payment - commission ₹{} will be deducted at end of day for booking {}", 
                    commissionAmount, booking.getBookingCode());
            }
        }
        
        bookingRepository.save(booking);
        
        // Check for auto-confirm: 3-4 seats booked within 30-45 mins
        checkAndAutoConfirmTrip(booking.getTrip());
        
        log.info("Confirmed booking {}", booking.getBookingCode());
        
        return toResponse(booking);
    }
    
    /**
     * Overloaded method for backward compatibility (defaults to ONLINE payment)
     */
    @Transactional
    public IntercityBookingResponse confirmBooking(Long bookingId, String razorpayPaymentId) throws ResourceNotFoundException {
        return confirmBooking(bookingId, razorpayPaymentId, IntercityPaymentMethod.ONLINE);
    }
    
    /**
     * Auto-confirm trip if 3-4 seats booked within 30-45 minutes
     */
    @Transactional
    public void checkAndAutoConfirmTrip(IntercityTrip trip) {
        if (trip == null || trip.getIsPrivate()) {
            return; // Skip for private bookings
        }
        
        // Count confirmed bookings in last 45 minutes
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(45);
        List<IntercityBooking> recentBookings = bookingRepository.findByTripIdAndStatusAndCreatedAtAfter(
                trip.getId(), 
                IntercityBookingStatus.CONFIRMED, 
                cutoffTime
        );
        
        int totalSeatsBooked = recentBookings.stream()
                .mapToInt(IntercityBooking::getSeatsBooked)
                .sum();
        
        // Auto-confirm if 3-4 seats booked within 30-45 mins
        if (totalSeatsBooked >= 3 && totalSeatsBooked <= 4) {
            LocalDateTime firstBookingTime = recentBookings.stream()
                    .map(IntercityBooking::getCreatedAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            
            long minutesSinceFirstBooking = java.time.Duration.between(firstBookingTime, LocalDateTime.now()).toMinutes();
            
            if (minutesSinceFirstBooking >= 30 && minutesSinceFirstBooking <= 45) {
                // Auto-confirm: Trip is ready for dispatch
                trip.setStatus(com.ridefast.ride_fast_backend.enums.IntercityTripStatus.MIN_REACHED);
                tripRepository.save(trip);
                
                log.info("Auto-confirmed trip {} - {} seats booked within {} minutes", 
                        trip.getTripCode(), totalSeatsBooked, minutesSinceFirstBooking);
            }
        }
    }
    
    /**
     * Cancel a booking
     */
    @Transactional
    public IntercityBookingResponse cancelBooking(Long bookingId, String reason) throws ResourceNotFoundException {
        IntercityBooking booking = getBookingById(bookingId);
        
        if (!booking.isCancellable()) {
            throw new IllegalStateException("Booking cannot be cancelled");
        }
        
        IntercityTrip trip = booking.getTrip();
        
        // Update booking
        booking.setStatus(IntercityBookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        
        // Cancel seat bookings
        for (IntercitySeatBooking seat : booking.getSeatBookings()) {
            seat.setStatus(IntercitySeatStatus.CANCELLED);
        }
        
        // Remove seats from trip
        tripService.removeSeatsFromTrip(trip, booking.getSeatsBooked());
        
        // Process refund if payment was completed
        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            booking.setRefundAmount(booking.getTotalAmount());
            booking.setRefundReason(reason);
            booking.setStatus(IntercityBookingStatus.REFUNDED);
            // TODO: Initiate actual refund via payment gateway
        }
        
        bookingRepository.save(booking);
        
        log.info("Cancelled booking {}: {}", booking.getBookingCode(), reason);
        
        return toResponse(booking);
    }
    
    /**
     * Get booking by ID
     */
    public IntercityBooking getBookingById(Long bookingId) throws ResourceNotFoundException {
        return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }
    
    /**
     * Get booking by code
     */
    public IntercityBooking getBookingByCode(String bookingCode) throws ResourceNotFoundException {
        return bookingRepository.findByBookingCode(bookingCode)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "code", bookingCode));
    }
    
    /**
     * Get user's bookings
     */
    public List<IntercityBookingResponse> getUserBookings(String userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get user's active bookings
     */
    public List<IntercityBookingResponse> getUserActiveBookings(String userId) {
        return bookingRepository.findActiveBookingsByUser(userId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get alternatives when minimum seats not met
     */
    public List<IntercityAlternativeDTO> getAlternatives(Long tripId) throws ResourceNotFoundException {
        IntercityTrip trip = tripService.getTripById(tripId);
        
        BigDecimal priceMultiplier = trip.getRoute() != null ? 
            trip.getRoute().getPriceMultiplier() : BigDecimal.ONE;
        
        return pricingService.getAlternatives(trip.getVehicleType(), priceMultiplier);
    }
    
    /**
     * Switch booking to alternative vehicle
     */
    @Transactional
    public IntercityBookingResponse switchToAlternative(
            Long bookingId, 
            IntercityVehicleType newVehicleType
    ) throws ResourceNotFoundException {
        IntercityBooking oldBooking = getBookingById(bookingId);
        
        if (oldBooking.getStatus() != IntercityBookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be switched");
        }
        
        // Cancel old booking
        cancelBooking(bookingId, "Switched to alternative vehicle");
        
        // Create new booking request
        IntercityBookingRequest request = IntercityBookingRequest.builder()
            .vehicleType(newVehicleType)
            .bookingType(oldBooking.getBookingType())
            .seatsToBook(oldBooking.getSeatsBooked())
            .pickupAddress(oldBooking.getTrip().getPickupAddress())
            .pickupLatitude(oldBooking.getTrip().getPickupLatitude())
            .pickupLongitude(oldBooking.getTrip().getPickupLongitude())
            .dropAddress(oldBooking.getTrip().getDropAddress())
            .dropLatitude(oldBooking.getTrip().getDropLatitude())
            .dropLongitude(oldBooking.getTrip().getDropLongitude())
            .contactPhone(oldBooking.getContactPhone())
            .specialInstructions(oldBooking.getSpecialInstructions())
            .build();
        
        // Create new booking
        IntercityBookingResponse newBooking = createBooking(oldBooking.getUser().getId(), request);
        
        log.info("Switched booking {} to vehicle type {}", oldBooking.getBookingCode(), newVehicleType);
        
        return newBooking;
    }
    
    /**
     * Convert booking to response DTO
     */
    public IntercityBookingResponse toResponse(IntercityBooking booking) {
        IntercityTrip trip = booking.getTrip();
        
        // Build trip info
        IntercityBookingResponse.TripInfo tripInfo = IntercityBookingResponse.TripInfo.builder()
            .tripId(trip.getId())
            .tripCode(trip.getTripCode())
            .vehicleType(trip.getVehicleType())
            .vehicleDisplayName(trip.getVehicleConfig() != null ? trip.getVehicleConfig().getDisplayName() : trip.getVehicleType().name())
            .tripStatus(trip.getStatus())
            .pickupAddress(trip.getPickupAddress())
            .pickupLatitude(trip.getPickupLatitude())
            .pickupLongitude(trip.getPickupLongitude())
            .dropAddress(trip.getDropAddress())
            .dropLatitude(trip.getDropLatitude())
            .dropLongitude(trip.getDropLongitude())
            .scheduledDeparture(trip.getScheduledDeparture())
            .countdownExpiry(trip.getCountdownExpiry())
            .totalSeats(trip.getTotalSeats())
            .seatsBooked(trip.getSeatsBooked())
            .availableSeats(trip.getAvailableSeats())
            .minSeats(trip.getMinSeats())
            .minSeatsMet(trip.isMinSeatsMet())
            .passengersOnboarded(trip.getPassengersOnboarded() != null ? trip.getPassengersOnboarded() : 0)
            .totalPrice(trip.getTotalPrice())
            .currentPerHeadPrice(trip.getCurrentPerHeadPrice())
            .build();
        
        // Add driver info if assigned - Hide phone number if booking is on HOLD
        if (trip.getDriver() != null) {
            String driverPhone = null;
            // Only show driver phone if booking is CONFIRMED or later (not HOLD)
            if (booking.getStatus() != IntercityBookingStatus.HOLD && 
                booking.getStatus() != IntercityBookingStatus.PENDING) {
                driverPhone = trip.getDriver().getMobile();
            }
            
            tripInfo.setDriver(IntercityBookingResponse.DriverInfo.builder()
                .driverId(trip.getDriver().getId())
                .name(trip.getDriver().getName())
                .phone(driverPhone)  // null if HOLD status
                .build());
        }
        
        // Build seat info
        List<IntercityBookingResponse.SeatInfo> seats = booking.getSeatBookings().stream()
            .map(s -> IntercityBookingResponse.SeatInfo.builder()
                .seatNumber(s.getSeatNumber())
                .status(s.getStatus())
                .pricePaid(s.getPricePaid())
                .passengerName(s.getPassengerName())
                .passengerPhone(s.getPassengerPhone())
                .build())
            .collect(Collectors.toList());
        
        return IntercityBookingResponse.builder()
            .bookingId(booking.getId())
            .bookingCode(booking.getBookingCode())
            .bookingType(booking.getBookingType())
            .bookingStatus(booking.getStatus())
            .seatsBooked(booking.getSeatsBooked())
            .totalAmount(booking.getTotalAmount())
            .perSeatAmount(booking.getPerSeatAmount())
            .otp(booking.getOtp())
            .otpVerified(booking.getOtpVerified() != null ? booking.getOtpVerified() : false)
            .otpVerifiedAt(booking.getOtpVerifiedAt())
            .passengersOnboarded(booking.getPassengersOnboarded() != null ? booking.getPassengersOnboarded() : 0)
            .paymentStatus(booking.getPaymentStatus())
            .razorpayOrderId(booking.getRazorpayOrderId())
            .trip(tripInfo)
            .seats(seats)
            .createdAt(booking.getCreatedAt())
            .confirmedAt(booking.getConfirmedAt())
            .build();
    }
    
    /**
     * Scheduled job to release expired seat locks
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void releaseExpiredLocks() {
        List<IntercitySeatBooking> expiredLocks = seatBookingRepository.findExpiredLocks(
            IntercitySeatStatus.LOCKED, LocalDateTime.now()
        );
        
        for (IntercitySeatBooking seat : expiredLocks) {
            log.info("Releasing expired seat lock for seat {} on trip {}", 
                seat.getSeatNumber(), seat.getTrip().getTripCode());
            
            seat.setStatus(IntercitySeatStatus.CANCELLED);
            seatBookingRepository.save(seat);
            
            // Update trip
            tripService.removeSeatsFromTrip(seat.getTrip(), 1);
            
            // Cancel the booking if all seats are released
            IntercityBooking booking = seat.getBooking();
            boolean allCancelled = booking.getSeatBookings().stream()
                .allMatch(s -> s.getStatus() == IntercitySeatStatus.CANCELLED);
            
            if (allCancelled) {
                booking.setStatus(IntercityBookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }
        }
    }
    
    /**
     * Scheduled job to deduct commission for cash payments (runs at end of day - 11 PM)
     * Deducts 5% commission from driver wallets for all cash payments completed today
     */
    @Scheduled(cron = "0 0 23 * * *") // 11 PM daily
    @Transactional
    public void deductCashCommissions() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now();
        
        // Find all cash payments completed today that haven't had commission deducted
        List<IntercityBooking> cashBookings = bookingRepository.findAll().stream()
            .filter(b -> b.getPaymentMethod() == IntercityPaymentMethod.CASH)
            .filter(b -> b.getPaymentStatus() == PaymentStatus.COMPLETED)
            .filter(b -> !Boolean.TRUE.equals(b.getCommissionDeducted()))
            .filter(b -> b.getConfirmedAt() != null && 
                        b.getConfirmedAt().isAfter(startOfDay) && 
                        b.getConfirmedAt().isBefore(endOfDay))
            .collect(Collectors.toList());
        
        int successCount = 0;
        int failCount = 0;
        
        for (IntercityBooking booking : cashBookings) {
            IntercityTrip trip = booking.getTrip();
            if (trip == null || trip.getDriver() == null) {
                continue;
            }
            
            try {
                BigDecimal commissionAmount = booking.getCommissionAmount();
                if (commissionAmount == null) {
                    commissionAmount = booking.getTotalAmount().multiply(COMMISSION_RATE);
                    booking.setCommissionAmount(commissionAmount);
                }
                
                walletService.debit(
                    com.ridefast.ride_fast_backend.enums.WalletOwnerType.DRIVER,
                    trip.getDriver().getId().toString(),
                    commissionAmount,
                    "INTERCITY_COMMISSION_CASH",
                    booking.getId().toString(),
                    String.format("5%% commission for cash payment - booking %s", booking.getBookingCode())
                );
                
                booking.setCommissionDeducted(true);
                booking.setCommissionDeductedAt(LocalDateTime.now());
                bookingRepository.save(booking);
                
                successCount++;
                log.info("Deducted cash commission ₹{} for booking {} (Driver: {})", 
                    commissionAmount, booking.getBookingCode(), trip.getDriver().getId());
            } catch (IllegalArgumentException e) {
                failCount++;
                log.error("Failed to deduct cash commission for booking {}: {}", 
                    booking.getBookingCode(), e.getMessage());
            }
        }
        
        if (cashBookings.size() > 0) {
            log.info("End-of-day cash commission deduction completed: {} successful, {} failed", 
                successCount, failCount);
        }
    }
    
    /**
     * Scheduled job to cancel pending bookings beyond payment timeout
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cancelTimedOutBookings() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<IntercityBooking> timedOut = bookingRepository.findPendingBookingsBeyondTimeout(timeout);
        
        for (IntercityBooking booking : timedOut) {
            try {
                log.info("Cancelling timed out booking {}", booking.getBookingCode());
                cancelBooking(booking.getId(), "Payment timeout");
            } catch (ResourceNotFoundException e) {
                log.warn("Booking not found while cancelling timeout: {}", booking.getId());
            }
        }
    }
    
    // ============== Private Helper Methods ==============
    
    private String generateBookingCode() {
        String code;
        do {
            code = "IC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }
}

