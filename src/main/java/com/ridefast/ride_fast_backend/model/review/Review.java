package com.ridefast.ride_fast_backend.model.review;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_driver", columnList = "driver_id"),
        @Index(name = "idx_review_user", columnList = "user_id")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Ride ride;

    @ManyToOne
    private MyUser user; // the rider being reviewed or the reviewer (see flags below)

    @ManyToOne
    private Driver driver; // the driver being reviewed or the reviewer (see flags below)

    // true if this review is for driver by user; false if for user by driver
    @Column(nullable = false)
    private Boolean forDriver;

    @Column(nullable = false)
    private Integer rating; // 1..5

    @Column(length = 2000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}


