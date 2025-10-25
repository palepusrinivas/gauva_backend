package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @GetMapping
  public ResponseEntity<Page<MyUser>> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(userRepository.findAll(pageable));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<MyUser> get(@PathVariable String userId) {
    return userRepository.findById(userId).map(ResponseEntity::ok)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public ResponseEntity<MyUser> create(@RequestBody MyUser body) {
    if (body.getPassword() != null) {
      body.setPassword(passwordEncoder.encode(body.getPassword()));
    }
    if (body.getRole() == null) body.setRole(UserRole.NORMAL_USER);
    MyUser saved = userRepository.save(body);
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }

  @PutMapping("/{userId}")
  public ResponseEntity<MyUser> update(@PathVariable String userId, @RequestBody MyUser body) {
    return userRepository.findById(userId)
        .map(existing -> {
          if (body.getFullName() != null) existing.setFullName(body.getFullName());
          if (body.getEmail() != null) existing.setEmail(body.getEmail());
          if (body.getPhone() != null) existing.setPhone(body.getPhone());
          if (body.getPassword() != null && !body.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(body.getPassword()));
          }
          if (body.getRole() != null) existing.setRole(body.getRole());
          return new ResponseEntity<>(userRepository.save(existing), HttpStatus.OK);
        })
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> delete(@PathVariable String userId) {
    if (!userRepository.existsById(userId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    userRepository.deleteById(userId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
