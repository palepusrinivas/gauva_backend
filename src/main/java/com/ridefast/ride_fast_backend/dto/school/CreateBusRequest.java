package com.ridefast.ride_fast_backend.dto.school;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateBusRequest {
	private String busNumber;
	private Integer capacity;
	private String type;
	private LocalDate rcExpiry;
	private LocalDate insuranceExpiry;
	private String photoUrl;
}


