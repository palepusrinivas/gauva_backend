package com.ridefast.ride_fast_backend.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReviewRequest {
    @NotNull
    private Long rideId;
    @NotNull
    private Long reviewerUserId;
    private Long reviewerDriverId;
    private Long revieweeUserId;
    private Long revieweeDriverId;
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
    private String comment;

    public String getComment() {
        return comment;
    }
}
