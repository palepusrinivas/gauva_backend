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
@Table(name = "students")
public class Student {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String studentClass;
	private String section;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_user_id")
	private MyUser parentUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id")
	private Branch branch;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bus_id")
	private Bus bus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stop_id")
	private Stop stop;

	@Column(columnDefinition = "text")
	private String address;

	@CreationTimestamp
	private LocalDateTime createdAt;
}


