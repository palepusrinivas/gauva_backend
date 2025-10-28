package com.ridefast.ride_fast_backend.controller.maps;

import com.ridefast.ride_fast_backend.service.maps.MapsKeyService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/maps/places")
@RequiredArgsConstructor
public class MapsPlacesController {

  private final MapsKeyService mapsKeyService;
  private final RestTemplate restTemplate = new RestTemplate();

  @GetMapping("/autocomplete")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> autocomplete(
      @RequestParam("input") String input,
      @RequestParam(value = "lat", required = false) Double lat,
      @RequestParam(value = "lng", required = false) Double lng,
      @RequestParam(value = "radius", required = false) Integer radiusMeters) {
    Optional<String> keyOpt = mapsKeyService.getServerKey();
    if (keyOpt.isEmpty()) return new ResponseEntity<>(Map.of("error", "Server key not configured"), HttpStatus.SERVICE_UNAVAILABLE);
    String key = keyOpt.get();

    StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/place/autocomplete/json");
    url.append("?input=").append(URLEncoder.encode(input, StandardCharsets.UTF_8));
    if (lat != null && lng != null) {
      url.append("&location=").append(lat).append(",").append(lng);
    }
    if (radiusMeters != null) {
      url.append("&radius=").append(radiusMeters);
    }
    url.append("&key=").append(URLEncoder.encode(key, StandardCharsets.UTF_8));

    Map<?,?> resp = restTemplate.getForObject(URI.create(url.toString()), Map.class);
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/details")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> details(@RequestParam("placeId") String placeId,
                                   @RequestParam(value = "fields", required = false) String fields) {
    Optional<String> keyOpt = mapsKeyService.getServerKey();
    if (keyOpt.isEmpty()) return new ResponseEntity<>(Map.of("error", "Server key not configured"), HttpStatus.SERVICE_UNAVAILABLE);
    String key = keyOpt.get();

    StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/place/details/json");
    url.append("?place_id=").append(URLEncoder.encode(placeId, StandardCharsets.UTF_8));
    if (fields != null && !fields.isBlank()) {
      url.append("&fields=").append(URLEncoder.encode(fields, StandardCharsets.UTF_8));
    } else {
      url.append("&fields=").append(URLEncoder.encode("geometry,name,formatted_address,place_id", StandardCharsets.UTF_8));
    }
    url.append("&key=").append(URLEncoder.encode(key, StandardCharsets.UTF_8));

    Map<?,?> resp = restTemplate.getForObject(URI.create(url.toString()), Map.class);
    return ResponseEntity.ok(resp);
  }
}
