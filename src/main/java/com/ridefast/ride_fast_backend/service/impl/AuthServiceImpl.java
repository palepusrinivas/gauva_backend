package com.ridefast.ride_fast_backend.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.dto.DriverSignUpRequest;
import com.ridefast.ride_fast_backend.dto.JwtResponse;
import com.ridefast.ride_fast_backend.dto.LoginRequest;
import com.ridefast.ride_fast_backend.dto.OtpLoginRequest;
import com.ridefast.ride_fast_backend.dto.SignUpRequest;
import com.ridefast.ride_fast_backend.dto.UserResponse;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.exception.UserException;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.RefreshToken;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.service.AuthService;
import com.ridefast.ride_fast_backend.service.CustomUserDetailsService;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.RefreshTokenService;
import com.ridefast.ride_fast_backend.service.ShortCodeService;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  @Autowired
  private final RefreshTokenService refreshTokenService;
  private final JwtTokenHelper jwtTokenHelper;
  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;

  private final DriverService driverService;
  private final PasswordEncoder passwordEncoder;
  private final ModelMapper modelMapper;
  private final ShortCodeService shortCodeService;

  @Override
  public UserResponse signUpUser(SignUpRequest request) throws UserException {

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      boolean emailPresent = userRepository.findByEmail(request.getEmail()).isPresent();
      if (emailPresent) throw new UserException("User already exists with this email");
    }
    boolean phonePresent = userRepository.findByPhone(request.getPhone()).isPresent();
    if (phonePresent) throw new UserException("User already exists with this phone");

    String encodedPassword = passwordEncoder.encode(request.getPassword());

    MyUser createdUser = modelMapper.map(request, MyUser.class);
    createdUser.setPassword(encodedPassword);
    createdUser.setRole(UserRole.NORMAL_USER);
    if (createdUser.getShortCode() == null || createdUser.getShortCode().isBlank()) {
      createdUser.setShortCode(shortCodeService.generateUserCode());
    }
    // Default language to satisfy NOT NULL constraint
    if (createdUser.getCurrentLanguageKey() == null || createdUser.getCurrentLanguageKey().isBlank()) {
      createdUser.setCurrentLanguageKey("en");
    }
    if (createdUser.getFailedAttempt() == null) {
      createdUser.setFailedAttempt(0);
    }
    if (createdUser.getIsActive() == null) {
      createdUser.setIsActive(true);
    }
    if (createdUser.getIsTempBlocked() == null) {
      createdUser.setIsTempBlocked(false);
    }
    if (createdUser.getLoyaltyPoints() == null) {
      createdUser.setLoyaltyPoints(0.0);
    }
    if (createdUser.getCreatedAt() == null) {
      createdUser.setCreatedAt(LocalDateTime.now());
    }

    MyUser savedUser = userRepository.save(createdUser);

    UserResponse userResponse = modelMapper.map(savedUser, UserResponse.class);
    return userResponse;

  }

  @Override
  public JwtResponse loginUser(LoginRequest request) throws ResourceNotFoundException {
    Authentication authentication = authenticate(request.getIdentifier(), request.getPassword());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getIdentifier());

    RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), request.getRole());

    String jwtToken = jwtTokenHelper.generateToken(userDetails.getUsername());

    JwtResponse response = JwtResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken.getRefreshToken())
        .type(request.getRole())
        .message("Login successfully : " + userDetails.getUsername())
        .build();
    return response;
  }

  private Authentication authenticate(String username, String password) {
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,
        password);
    try {
      Authentication authentication = authenticationManager.authenticate(authenticationToken);
      return authentication;
    } catch (BadCredentialsException e) {
      throw new BadCredentialsException("invalid username or password");
    }
  }

  @Override
  public JwtResponse loginUserWithOtp(OtpLoginRequest request) throws ResourceNotFoundException {
    try {
      FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
      String firebaseUid = decoded.getUid();
      String phone = null;
      Object claimPhone = decoded.getClaims().get("phone_number");
      if (claimPhone instanceof String cp && !cp.isBlank()) {
        phone = cp;
      } else {
        // Fallback to user lookup
        String fetched = FirebaseAuth.getInstance().getUser(firebaseUid).getPhoneNumber();
        if (fetched != null && !fetched.isBlank()) {
          phone = fetched;
        }
      }
      if (phone == null || phone.isBlank()) {
        throw new ResourceNotFoundException("FirebaseToken", "phone", "missing");
      }
      Optional<MyUser> optional = userRepository.findByPhone(phone);
      if (optional.isEmpty()) {
        throw new ResourceNotFoundException("User", "phone", phone);
      }
      MyUser user = optional.get();
      if (user.getFirebaseUid() == null || user.getFirebaseUid().isBlank()) {
        user.setFirebaseUid(firebaseUid);
      }
      if (user.getPhoneVerifiedAt() == null) {
        user.setPhoneVerifiedAt(LocalDateTime.now());
      }
      userRepository.save(user);

      String principal = (user.getEmail() != null && !user.getEmail().isBlank()) ? user.getEmail() : user.getPhone();
      UserDetails userDetails = userDetailsService.loadUserByUsername(principal);
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername(), request.getRole());
      String jwtToken = jwtTokenHelper.generateToken(userDetails.getUsername());
      return JwtResponse.builder()
          .accessToken(jwtToken)
          .refreshToken(refreshToken.getRefreshToken())
          .type(request.getRole())
          .message("Login successfully via OTP: " + userDetails.getUsername())
          .build();
    } catch (Exception ex) {
      throw new ResourceNotFoundException("OTP", "verification", ex.getMessage());
    }
  }

  @Override
  public DriverResponse registerDriver(DriverSignUpRequest request) {
    Driver registeredDriver = driverService.registerDriver(request);
    return modelMapper.map(registeredDriver, DriverResponse.class);
  }

}
