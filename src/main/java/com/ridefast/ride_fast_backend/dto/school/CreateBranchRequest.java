package com.ridefast.ride_fast_backend.dto.school;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBranchRequest {
	private String name;
    private String branchId;
	private String address;
	private String city;
	private String state;
	private String pincode;
	private Double latitude;
	private Double longitude;
	private String subscriptionPlan;
}


