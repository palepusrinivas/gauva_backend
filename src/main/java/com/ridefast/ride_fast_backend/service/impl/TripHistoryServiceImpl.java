package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.dto.RideDto;
import com.ridefast.ride_fast_backend.dto.UnifiedTripHistoryDto;
import com.ridefast.ride_fast_backend.dto.UserResponse;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.intercity.IntercityBooking;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.repository.intercity.IntercityBookingRepository;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.TripHistoryService;
import com.ridefast.ride_fast_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripHistoryServiceImpl implements TripHistoryService {

    private final RideRepository rideRepository;
    private final IntercityBookingRepository intercityBookingRepository;
    private final UserService userService;
    private final DriverService driverService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UnifiedTripHistoryDto> getUnifiedTripHistory(
            String jwtToken,
            int page,
            int size
    ) throws UserException, ResourceNotFoundException {
        // Determine if requester is user or driver
        MyUser user = null;
        Driver driver = null;
        UserRole role = null;

        try {
            user = userService.getRequestedUserProfile(jwtToken);
            role = user.getRole();
        } catch (Exception e) {
            try {
                driver = driverService.getRequestedDriverProfile(jwtToken);
                role = UserRole.DRIVER;
            } catch (Exception ex) {
                throw new ResourceNotFoundException("User/Driver", "token", "Invalid token");
            }
        }

        // Fetch regular rides
        Page<Ride> regularRides = Page.empty();
        if (user != null) {
            Pageable ridePageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime", "id"));
            regularRides = rideRepository.findByUser_Id(user.getId(), ridePageable);
        } else if (driver != null) {
            Pageable ridePageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime", "id"));
            regularRides = rideRepository.findByDriver_Id(driver.getId(), ridePageable);
        }

        // Fetch intercity bookings
        List<IntercityBooking> intercityBookings = new ArrayList<>();
        if (user != null) {
            // Get all bookings for this user with trip and driver eagerly loaded
            intercityBookings = intercityBookingRepository.findByUserIdWithTripAndDriver(user.getId());
        } else if (driver != null) {
            // For drivers, get all intercity bookings with trip and driver eagerly loaded
            // Then filter by driver's trips
            List<IntercityBooking> allBookings = intercityBookingRepository.findAllWithTripAndDriver();
            final Long driverId = driver.getId();
            intercityBookings = allBookings.stream()
                    .filter(booking -> {
                        try {
                            return booking.getTrip() != null 
                                    && booking.getTrip().getDriver() != null 
                                    && booking.getTrip().getDriver().getId().equals(driverId);
                        } catch (Exception e) {
                            log.warn("Error accessing trip/driver for booking {}: {}", booking.getId(), e.getMessage());
                            return false;
                        }
                    })
                    .sorted(Comparator.comparing(IntercityBooking::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        // Convert to DTOs
        List<UnifiedTripHistoryDto> unifiedTrips = new ArrayList<>();

        // Add regular rides
        for (Ride ride : regularRides.getContent()) {
            UnifiedTripHistoryDto dto = convertRideToUnified(ride, user != null);
            unifiedTrips.add(dto);
        }

        // Add intercity bookings
        for (IntercityBooking booking : intercityBookings) {
            UnifiedTripHistoryDto dto = convertIntercityBookingToUnified(booking, user != null);
            unifiedTrips.add(dto);
        }

        // Sort by start time (most recent first)
        unifiedTrips.sort((a, b) -> {
            LocalDateTime timeA = a.getStartTime() != null ? a.getStartTime() : a.getCreatedAt();
            LocalDateTime timeB = b.getStartTime() != null ? b.getStartTime() : b.getCreatedAt();
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA); // Descending order
        });

        // Apply pagination manually
        int totalElements = unifiedTrips.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        List<UnifiedTripHistoryDto> paginatedTrips = start < totalElements 
                ? unifiedTrips.subList(start, end) 
                : new ArrayList<>();

        // Create page response
        Pageable pageable = PageRequest.of(page, size);
        return new org.springframework.data.domain.PageImpl<>(
                paginatedTrips,
                pageable,
                totalElements
        );
    }

    private UnifiedTripHistoryDto convertRideToUnified(Ride ride, boolean isUserView) {
        return UnifiedTripHistoryDto.builder()
                .tripType("REGULAR")
                .id(ride.getId())
                .code(ride.getShortCode())
                .startTime(ride.getStartTime())
                .endTime(ride.getEndTime())
                .createdAt(ride.getStartTime() != null ? ride.getStartTime() : null)
                .pickupAddress(ride.getPickupArea())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .dropAddress(ride.getDestinationArea())
                .dropLatitude(ride.getDestinationLatitude())
                .dropLongitude(ride.getDestinationLongitude())
                .rideStatus(ride.getStatus())
                .fare(ride.getFare())
                .paymentDetails(ride.getPaymentDetails())
                .distance(ride.getDistance())
                .duration(ride.getDuration())
                .otp(ride.getOtp())
                .user(isUserView ? null : (ride.getUser() != null ? modelMapper.map(ride.getUser(), UserResponse.class) : null))
                .driver(isUserView ? (ride.getDriver() != null ? modelMapper.map(ride.getDriver(), DriverResponse.class) : null) : null)
                .build();
    }

    private UnifiedTripHistoryDto convertIntercityBookingToUnified(IntercityBooking booking, boolean isUserView) {
        var trip = booking.getTrip();
        if (trip == null) {
            log.warn("Booking {} has no associated trip", booking.getId());
            // Return minimal DTO if trip is missing
            return UnifiedTripHistoryDto.builder()
                    .tripType("INTERCITY")
                    .id(booking.getId())
                    .code(booking.getBookingCode())
                    .createdAt(booking.getCreatedAt())
                    .bookingStatus(booking.getStatus())
                    .totalAmount(booking.getTotalAmount())
                    .perSeatAmount(booking.getPerSeatAmount())
                    .paymentStatus(booking.getPaymentStatus())
                    .razorpayOrderId(booking.getRazorpayOrderId())
                    .bookingType(booking.getBookingType())
                    .seatsBooked(booking.getSeatsBooked())
                    .otpVerified(booking.getOtpVerified())
                    .otpVerifiedAt(booking.getOtpVerifiedAt())
                    .passengersOnboarded(booking.getPassengersOnboarded())
                    .build();
        }
        
        return UnifiedTripHistoryDto.builder()
                .tripType("INTERCITY")
                .id(booking.getId())
                .code(booking.getBookingCode())
                .startTime(trip.getScheduledDeparture())
                .endTime(trip.getActualArrival())
                .createdAt(booking.getCreatedAt())
                .pickupAddress(trip.getPickupAddress())
                .pickupLatitude(trip.getPickupLatitude())
                .pickupLongitude(trip.getPickupLongitude())
                .dropAddress(trip.getDropAddress())
                .dropLatitude(trip.getDropLatitude())
                .dropLongitude(trip.getDropLongitude())
                .bookingStatus(booking.getStatus())
                .tripStatus(trip.getStatus())
                .totalAmount(booking.getTotalAmount())
                .perSeatAmount(booking.getPerSeatAmount())
                .paymentStatus(booking.getPaymentStatus())
                .razorpayOrderId(booking.getRazorpayOrderId())
                .bookingType(booking.getBookingType())
                .seatsBooked(booking.getSeatsBooked())
                .vehicleType(trip.getVehicleType())
                .totalSeats(trip.getTotalSeats())
                .availableSeats(trip.getTotalSeats() != null && trip.getSeatsBooked() != null 
                        ? trip.getTotalSeats() - trip.getSeatsBooked() 
                        : null)
                .otpVerified(booking.getOtpVerified())
                .otpVerifiedAt(booking.getOtpVerifiedAt())
                .passengersOnboarded(booking.getPassengersOnboarded())
                .intercityDriver(trip.getDriver() != null 
                        ? modelMapper.map(trip.getDriver(), DriverResponse.class) 
                        : null)
                .build();
    }
}
