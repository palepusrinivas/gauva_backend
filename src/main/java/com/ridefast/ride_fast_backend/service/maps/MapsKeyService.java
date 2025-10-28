package com.ridefast.ride_fast_backend.service.maps;

import com.ridefast.ride_fast_backend.model.ApiKey;
import com.ridefast.ride_fast_backend.repository.ApiKeyRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MapsKeyService {
  public static final String CLIENT_KEY_NAME = "GOOGLE_MAPS_CLIENT_KEY";
  public static final String SERVER_KEY_NAME = "GOOGLE_PLACES_SERVER_KEY";

  private final ApiKeyRepository apiKeyRepository;

  public Optional<String> getClientKey() {
    return apiKeyRepository.findByName(CLIENT_KEY_NAME).map(ApiKey::getValue);
  }

  public Optional<String> getServerKey() {
    return apiKeyRepository.findByName(SERVER_KEY_NAME).map(ApiKey::getValue);
  }
}
