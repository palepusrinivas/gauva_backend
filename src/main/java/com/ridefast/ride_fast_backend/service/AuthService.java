package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.dto.DriverSignUpRequest;
import com.ridefast.ride_fast_backend.dto.JwtResponse;
import com.ridefast.ride_fast_backend.dto.LoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpLoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendResponse;
import com.ridefast.ride_fast_backend.dto.OtpVerifyRequest;
import com.ridefast.ride_fast_backend.dto.SignUpRequest;
import com.ridefast.ride_fast_backend.dto.UserResponse;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;

public interface AuthService {
  UserResponse signUpUser(SignUpRequest request) throws UserException;

  JwtResponse loginUser(LoginRequest request) throws ResourceNotFoundException;

  JwtResponse loginUserWithOtp(OtpLoginRequest request) throws ResourceNotFoundException;

  OtpSendResponse sendOtp(OtpSendRequest request) throws ResourceNotFoundException;

  JwtResponse verifyOtp(OtpVerifyRequest request) throws ResourceNotFoundException;

  DriverResponse registerDriver(DriverSignUpRequest request);
}
