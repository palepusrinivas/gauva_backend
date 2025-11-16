package com.ridefast.ride_fast_backend.dto.school;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInstitutionRequest {
	@NotBlank
	private String name;
    private String uniqueId;
    private String primaryContactName;
	private String primaryContactPhone;
	private String email;
	private String gstNumber;
}


