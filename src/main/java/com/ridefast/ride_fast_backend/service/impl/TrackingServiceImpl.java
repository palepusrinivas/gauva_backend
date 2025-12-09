package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.dto.LocationUpdate;
import com.ridefast.ride_fast_backend.service.RealtimeService;
import com.ridefast.ride_fast_backend.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final RealtimeService realtimeService;

  private String key(Long rideId) {
    return "ride:last:" + rideId;
  }

  private String userKey(String userId) {
    return "user:last:" + userId;
  }

  @Override
  public void saveLastLocation(Long rideId, LocationUpdate update) {
    redisTemplate.opsForValue().set(key(rideId), update);
    
    // Broadcast location update
    try {
      if (update.getLat() != null && update.getLng() != null) {
        // Get driverId from ride if available (would need rideRepository injected)
        // For now, broadcast with null driverId - can be enhanced later
        Double heading = update.getHeading() != null ? update.getHeading().doubleValue() : null;
        realtimeService.broadcastDriverLocation(rideId, null, update.getLat(), update.getLng(), heading);
      }
    } catch (Exception e) {
      log.warn("Failed to broadcast location update: {}", e.getMessage());
    }
  }

  @Override
  public LocationUpdate getLastLocation(Long rideId) {
    Object o = redisTemplate.opsForValue().get(key(rideId));
    if (o instanceof LocationUpdate lu) return lu;
    return null;
  }

  @Override
  public void saveUserLastLocation(String userId, LocationUpdate update) {
    redisTemplate.opsForValue().set(userKey(userId), update);
  }

  @Override
  public LocationUpdate getUserLastLocation(String userId) {
    Object o = redisTemplate.opsForValue().get(userKey(userId));
    if (o instanceof LocationUpdate lu) return lu;
    return null;
  }
}
