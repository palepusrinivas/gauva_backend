package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.dto.UnifiedTripHistoryDto;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import org.springframework.data.domain.Page;

public interface TripHistoryService {
    Page<UnifiedTripHistoryDto> getUnifiedTripHistory(
            String jwtToken,
            int page,
            int size
    ) throws UserException, ResourceNotFoundException;
}
