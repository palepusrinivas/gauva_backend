package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "stops")
public class Stop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_id")
	private Route route;

	private String name;
	private Double latitude;
	private Double longitude;
	private Integer stopOrder;
	private Integer etaMinutesFromPrev;

	@CreationTimestamp
	private LocalDateTime createdAt;
}


