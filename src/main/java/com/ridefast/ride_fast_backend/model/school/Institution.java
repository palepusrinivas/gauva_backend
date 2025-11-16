package com.ridefast.ride_fast_backend.model.school;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "institutions")
public class Institution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;
    @Column( unique = true)
    private String uniqueId;
	private String primaryContactName;
	private String primaryContactPhone;
	private String email;
	private String gstNumber;
	


	@CreationTimestamp
	private LocalDateTime createdAt;
}


