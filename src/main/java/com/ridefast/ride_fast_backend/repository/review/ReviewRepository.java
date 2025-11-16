package com.ridefast.ride_fast_backend.repository.review;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByDriverOrderByCreatedAtDesc(Driver driver);
    List<Review> findByUserOrderByCreatedAtDesc(MyUser user);
}


