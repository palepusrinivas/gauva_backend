package com.ridefast.ride_fast_backend.controller.review;

import com.ridefast.ride_fast_backend.dto.review.CreateReviewRequest;
import com.ridefast.ride_fast_backend.model.review.Review;
import com.ridefast.ride_fast_backend.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Review> create(@RequestBody @Validated CreateReviewRequest req) {
        return new ResponseEntity<>(reviewService.create(req), HttpStatus.CREATED);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Review>> listByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(reviewService.listByDriver(driverId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> listByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.listByUser(userId));
    }
}


