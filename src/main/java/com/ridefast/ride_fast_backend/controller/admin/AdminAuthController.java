package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.dto.AdminLoginRequest;
import com.ridefast.ride_fast_backend.model.AdminUser;
import com.ridefast.ride_fast_backend.repository.AdminUserRepository;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuthController {

  private final AdminUserRepository adminUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenHelper jwtTokenHelper;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody @Valid AdminLoginRequest request) {
    AdminUser admin = adminUserRepository.findByUsername(request.getUsername()).orElse(null);
    if (admin == null) {
      return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }
    if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
      return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(admin.getUsername(), request.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String jwt = jwtTokenHelper.generateToken(admin.getUsername());
    return ResponseEntity.ok(java.util.Map.of(
        "accessToken", jwt,
        "role", admin.getRole(),
        "username", admin.getUsername()
    ));
  }
}
