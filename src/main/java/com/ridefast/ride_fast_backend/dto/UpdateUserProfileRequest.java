package com.ridefast.ride_fast_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImage;
    private String currentLanguageKey;
}

