package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "scratch_plans")
public class ScratchPlan {
	@Id
	private String planType;
	private Integer scratchValue;
}


