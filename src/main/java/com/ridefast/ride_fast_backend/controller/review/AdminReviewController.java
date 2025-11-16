package com.ridefast.ride_fast_backend.controller.review;

import com.ridefast.ride_fast_backend.model.review.Review;
import com.ridefast.ride_fast_backend.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<List<Review>> listAll() {
        return ResponseEntity.ok(reviewRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!reviewRepository.existsById(id)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        reviewRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}


