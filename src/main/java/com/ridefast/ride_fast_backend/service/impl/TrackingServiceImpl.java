package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.dto.LocationUpdate;
import com.ridefast.ride_fast_backend.service.TrackingService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TrackingServiceImpl implements TrackingService {

  private final RedisTemplate<String, Object> redisTemplate;

  public TrackingServiceImpl(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  private String key(Long rideId) {
    return "ride:last:" + rideId;
  }

  @Override
  public void saveLastLocation(Long rideId, LocationUpdate update) {
    redisTemplate.opsForValue().set(key(rideId), update);
  }

  @Override
  public LocationUpdate getLastLocation(Long rideId) {
    Object o = redisTemplate.opsForValue().get(key(rideId));
    if (o instanceof LocationUpdate lu) return lu;
    return null;
  }
}
