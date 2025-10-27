package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.model.License;
import com.ridefast.ride_fast_backend.model.Vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverSignUpRequest {
  private String name;
  private String email;
  private String password;
  private String mobile;
  private double latitude;
  private double longitude;
  private License license;
  private Vehicle vehicle;
  private String driverId;
  private String accountHolderName;
  private String bankName;
  private String accountNumber;
  private String ifscCode;
  private String upiId;
  private String bankAddress;
  private String bankMobile;

    public void setDriverId() {
        Random rd = new Random();
        int number = 1000000000 + rd.nextInt(1000000000); // ensures 10-digit number
        this.driverId = String.valueOf(number);
    }

}
