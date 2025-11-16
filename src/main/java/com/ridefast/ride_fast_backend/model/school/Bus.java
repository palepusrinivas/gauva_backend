package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "buses")
public class Bus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id")
	private Branch branch;

	@Column(nullable = false, length = 64)
	private String busNumber;

	private Integer capacity;

	@Column(length = 16)
	private String type; // morning/evening/both

	private LocalDate rcExpiry;
	private LocalDate insuranceExpiry;
	private String photoUrl;

	@CreationTimestamp
	private LocalDateTime createdAt;
}


