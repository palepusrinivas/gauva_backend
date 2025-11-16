package com.ridefast.ride_fast_backend.model.school;

import com.ridefast.ride_fast_backend.model.MyUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "alerts")
public class AlertLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private MyUser user;

	private String type;

	@Column(columnDefinition = "jsonb")
	private String payload;

	private LocalDateTime sentAt;

	@CreationTimestamp
	private LocalDateTime createdAt;
}


