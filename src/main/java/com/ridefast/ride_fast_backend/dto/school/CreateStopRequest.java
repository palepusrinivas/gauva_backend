package com.ridefast.ride_fast_backend.dto.school;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStopRequest {
	private String name;
	private Double latitude;
	private Double longitude;
	private Integer stopOrder;
	private Integer etaMinutesFromPrev;
}


