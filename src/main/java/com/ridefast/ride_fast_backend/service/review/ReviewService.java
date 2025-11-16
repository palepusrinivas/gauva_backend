package com.ridefast.ride_fast_backend.service.review;

import com.ridefast.ride_fast_backend.dto.review.CreateReviewRequest;
import com.ridefast.ride_fast_backend.model.review.Review;

import java.util.List;

public interface ReviewService {
    Review create(CreateReviewRequest req);
    List<Review> listByDriver(Long driverId);
    List<Review> listByUser(Long userId);
}


