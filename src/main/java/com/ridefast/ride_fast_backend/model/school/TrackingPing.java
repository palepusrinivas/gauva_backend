package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tracking")
public class TrackingPing {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bus_id")
	private Bus bus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id")
	private SchoolDriver driver;

	private Double latitude;
	private Double longitude;
	private Float speed;
	private Float heading;
	private LocalDateTime createdAt = LocalDateTime.now();
}


