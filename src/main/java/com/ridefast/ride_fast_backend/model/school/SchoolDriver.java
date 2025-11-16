package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "drivers")
public class SchoolDriver {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@Column(unique = true, length = 20)
	private String phone;

	private String licenseNumber;

	@Column(columnDefinition = "jsonb")
	private String kycDocs;

	private Boolean isActive = Boolean.FALSE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_bus_id")
	private Bus assignedBus;

	@CreationTimestamp
	private LocalDateTime createdAt;
}


