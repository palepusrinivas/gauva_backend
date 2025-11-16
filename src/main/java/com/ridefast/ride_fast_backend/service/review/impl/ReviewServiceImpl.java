package com.ridefast.ride_fast_backend.service.review.impl;

import com.ridefast.ride_fast_backend.dto.review.CreateReviewRequest;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.review.Review;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.review.ReviewRepository;
import com.ridefast.ride_fast_backend.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Review create(CreateReviewRequest req) {
        Ride ride = rideRepository.findById(req.getRideId()).orElseThrow();
        MyUser reviewerUser = req.getReviewerUserId() != null ? userRepository.findById(String.valueOf(req.getReviewerUserId())).orElse(null) : null;
        Driver reviewerDriver = req.getReviewerDriverId() != null ? driverRepository.findById(req.getReviewerDriverId()).orElse(null) : null;
        MyUser revieweeUser = req.getRevieweeUserId() != null ? userRepository.findById(String.valueOf(req.getRevieweeUserId())).orElse(null) : null;
        Driver revieweeDriver = req.getRevieweeDriverId() != null ? driverRepository.findById(req.getRevieweeDriverId()).orElse(null) : null;

        boolean forDriver = revieweeDriver != null;

        Review r = Review.builder()
                .ride(ride)
                .user(revieweeUser)
                .driver(revieweeDriver)
                .forDriver(forDriver)
                .rating(req.getRating())
                .comment(req.getComment())
                .createdAt(LocalDateTime.now())
                .build();
        return reviewRepository.save(r);
    }

    @Override
    public List<Review> listByDriver(Long driverId) {
        Driver d = driverRepository.findById(driverId).orElseThrow();
        return reviewRepository.findByDriverOrderByCreatedAtDesc(d);
    }

    @Override
    public List<Review> listByUser(Long userId) {
        MyUser u = userRepository.findById(String.valueOf(userId)).orElseThrow();
        return reviewRepository.findByUserOrderByCreatedAtDesc(u);
    }
}


