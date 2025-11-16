package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "branches")
public class Branch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "institution_id")
	private Institution institution;

	private String name;
    private String branchId;
	private String address;
	private String city;
	private String state;
	private String pincode;
	private Double latitude;
	private Double longitude;
	private String subscriptionPlan; // Basic/Standard/Premium

	@CreationTimestamp
	private LocalDateTime createdAt;
}


