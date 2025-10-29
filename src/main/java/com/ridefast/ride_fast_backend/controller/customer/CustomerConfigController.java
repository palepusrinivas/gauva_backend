package com.ridefast.ride_fast_backend.controller.customer;

import com.ridefast.ride_fast_backend.service.maps.MapsKeyService;
import com.ridefast.ride_fast_backend.model.Zone;
import com.ridefast.ride_fast_backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/config")
@RequiredArgsConstructor
public class CustomerConfigController {

  private final MapsKeyService mapsKeyService;
  private final ZoneRepository zoneRepository;
  private final RestTemplate restTemplate = new RestTemplate();

  // Compatibility for: /api/customer/config/place-api-autocomplete?search_text=...
  @GetMapping("/place-api-autocomplete")
  public ResponseEntity<?> autocomplete(
      @RequestParam(name = "search_text") String input,
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

  // Compatibility for: /api/customer/config/place-api-details?place_id=...
  @GetMapping("/place-api-details")
  public ResponseEntity<?> details(
      @RequestParam(value = "place_id", required = false) String placeIdSnake,
      @RequestParam(value = "placeId", required = false) String placeIdCamel,
      @RequestParam(value = "fields", required = false) String fields) {
    String placeId = placeIdSnake != null ? placeIdSnake : placeIdCamel;
    if (placeId == null || placeId.isBlank()) {
      return new ResponseEntity<>(Map.of("error", "placeId is required"), HttpStatus.BAD_REQUEST);
    }

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

  // New: /api/customer/config/geocode-api?lat=..&lng=..
  @GetMapping("/geocode-api")
  public ResponseEntity<?> reverseGeocode(
      @RequestParam("lat") Double lat,
      @RequestParam("lng") Double lng) {
    if (lat == null || lng == null) {
      return new ResponseEntity<>(Map.of("error", "lat and lng are required"), HttpStatus.BAD_REQUEST);
    }
    Optional<String> keyOpt = mapsKeyService.getServerKey();
    if (keyOpt.isEmpty()) return new ResponseEntity<>(Map.of("error", "Server key not configured"), HttpStatus.SERVICE_UNAVAILABLE);
    String key = keyOpt.get();

    StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json");
    url.append("?latlng=").append(lat).append(",").append(lng);
    url.append("&key=").append(URLEncoder.encode(key, StandardCharsets.UTF_8));

    Map<?,?> resp = restTemplate.getForObject(URI.create(url.toString()), Map.class);
    return ResponseEntity.ok(resp);
  }

  // New: /api/customer/config/get-zone-id?lat=..&lng=..
  @GetMapping("/get-zone-id")
  public ResponseEntity<?> getZoneId(
      @RequestParam("lat") Double lat,
      @RequestParam("lng") Double lng) {
    if (lat == null || lng == null) {
      return new ResponseEntity<>(Map.of("error", "lat and lng are required"), HttpStatus.BAD_REQUEST);
    }

    GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
    Point point = gf.createPoint(new Coordinate(lng, lat)); // x=lng, y=lat
    WKTReader reader = new WKTReader(gf);

    for (Zone z : zoneRepository.findAll()) {
      if (z == null || !z.isActive()) continue;
      String wkt = z.getPolygonWkt();
      if (wkt == null || wkt.isBlank()) continue;
      try {
        Geometry poly = reader.read(wkt);
        if (poly != null && (poly.contains(point) || poly.covers(point))) {
          return ResponseEntity.ok(Map.of(
              "zoneId", z.getReadableId(),
              "name", z.getName()
          ));
        }
      } catch (Exception ignore) {
        // skip invalid polygons
      }
    }

    return new ResponseEntity<>(Map.of("error", "Zone not found for given location"), HttpStatus.NOT_FOUND);
  }
}
