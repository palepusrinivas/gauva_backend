package com.ridefast.ride_fast_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.dto.DriverSignUpRequest;
import com.ridefast.ride_fast_backend.dto.GoogleLoginRequest;
import com.ridefast.ride_fast_backend.dto.JwtResponse;
import com.ridefast.ride_fast_backend.dto.LoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendRequest;
import com.ridefast.ride_fast_backend.dto.OtpSendResponse;
import com.ridefast.ride_fast_backend.dto.OtpVerifyRequest;
import com.ridefast.ride_fast_backend.dto.SignUpRequest;
import com.ridefast.ride_fast_backend.dto.UserResponse;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.service.AuthService;
import com.ridefast.ride_fast_backend.service.RefreshTokenService;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;
  private final JwtTokenHelper jwtTokenHelper;

  @PostMapping("/register/user")
  public ResponseEntity<UserResponse> signUpHandler(@RequestBody @Valid SignUpRequest signUpRequest) throws UserException {
    UserResponse userResponse = authService.signUpUser(signUpRequest);
    return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<?> loginHandler(@RequestBody @Valid LoginRequest loginRequest) throws org.springframework.web.bind.MethodArgumentNotValidException {
    try {
      JwtResponse jwtResponse = authService.loginUser(loginRequest);
      return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
    } catch (ResourceNotFoundException e) {
      return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (UserException e) {
      return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (org.springframework.security.authentication.BadCredentialsException e) {
      return new ResponseEntity<>(Map.of("error", "Invalid credentials"), HttpStatus.UNAUTHORIZED);
    } catch (Exception e) {
      return new ResponseEntity<>(Map.of("error", "Login failed: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/login/otp")
  public ResponseEntity<?> otpLoginHandler(@RequestBody Map<String, Object> request)
      throws ResourceNotFoundException {
    // Handle both send OTP and verify OTP requests
    // If request contains only phoneNumber, it's a send/resend OTP request
    // If request contains idToken, it's a verify OTP request
    
    try {
      if (request.containsKey("phoneNumber") && !request.containsKey("idToken")) {
        // Send/Resend OTP request
        OtpSendRequest otpSendRequest = new OtpSendRequest();
        otpSendRequest.setPhoneNumber((String) request.get("phoneNumber"));
        OtpSendResponse response = authService.sendOtp(otpSendRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
      } else if (request.containsKey("idToken")) {
        // Verify OTP request
        OtpVerifyRequest otpVerifyRequest = new OtpVerifyRequest();
        otpVerifyRequest.setIdToken((String) request.get("idToken"));
        if (request.containsKey("role")) {
          String roleStr = request.get("role").toString();
          try {
            otpVerifyRequest.setRole(UserRole.valueOf(roleStr));
          } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Map.of("error", "Invalid role"), HttpStatus.BAD_REQUEST);
          }
        } else {
          // Default to NORMAL_USER if role not provided
          otpVerifyRequest.setRole(UserRole.NORMAL_USER);
        }
        JwtResponse jwtResponse = authService.verifyOtp(otpVerifyRequest);
        return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
      } else {
        return new ResponseEntity<>(Map.of("error", "Invalid request format. Provide either 'phoneNumber' or 'idToken'"), HttpStatus.BAD_REQUEST);
      }
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("/register/driver")
  public ResponseEntity<DriverResponse> registerDriver(@RequestBody @Valid DriverSignUpRequest signUpRequest) {
    DriverResponse driverResponse = authService.registerDriver(signUpRequest);
    return new ResponseEntity<>(driverResponse, HttpStatus.CREATED);
  }

  @PostMapping("/logout/user")
  public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String authHeader) throws ResourceNotFoundException {
    String username = jwtTokenHelper.getUsernameFromToken(authHeader);
    refreshTokenService.revokeByUsername(username, UserRole.NORMAL_USER);
    return ResponseEntity.ok(Map.of("status", "logged_out"));
  }

  @PostMapping("/logout/driver")
  public ResponseEntity<?> logoutDriver(@RequestHeader("Authorization") String authHeader) throws ResourceNotFoundException {
    String username = jwtTokenHelper.getUsernameFromToken(authHeader);
    refreshTokenService.revokeByUsername(username, UserRole.DRIVER);
    return ResponseEntity.ok(Map.of("status", "logged_out"));
  }

  /**
   * Google social login endpoint
   * Only available for users (NORMAL_USER), not for drivers
   * 
   * POST /api/v1/auth/login/google
   * 
   * Request body:
   * {
   *   "idToken": "firebase_id_token",
   *   "name": "optional - user's name",
   *   "email": "optional - user's email (if not in token)",
   *   "phone": "optional - user's phone (if not in token)"
   * }
   */
  @PostMapping("/login/google")
  public ResponseEntity<?> googleLoginHandler(@RequestBody @Valid GoogleLoginRequest googleLoginRequest) {
    try {
      JwtResponse jwtResponse = authService.loginWithGoogle(googleLoginRequest);
      return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
    } catch (ResourceNotFoundException e) {
      return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (UserException e) {
      return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      log.error("Google login error: {}", e.getMessage(), e);
      return new ResponseEntity<>(Map.of("error", "Google login failed: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
