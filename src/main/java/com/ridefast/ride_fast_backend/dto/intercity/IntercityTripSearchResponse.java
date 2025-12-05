package com.ridefast.ride_fast_backend.dto.intercity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for intercity trip search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityTripSearchResponse {
    
    /** Available vehicle options for new trip */
    private List<IntercityVehicleOptionDTO> vehicleOptions;
    
    /** Existing trips that can be joined (for share pool) */
    private List<IntercityTripDTO> availableTrips;
    
    /** Route information */
    private RouteInfo route;
    
    /** Recommendations */
    private String recommendedVehicle;
    private String recommendationReason;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteInfo {
        private Long routeId;
        private String routeCode;
        private String originName;
        private String destinationName;
        private Double distanceKm;
        private Integer estimatedDurationMinutes;
    }
}

