package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.service.trip.TripDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    private static final int TELEMETRY_LIMIT = 1000;

    @Override
    public TripResponse getLastTrip(String vehicleId) {

        List<TelemetryPoint> telemetryPoints = telemetryRepository.findRecentPoints(vehicleId, TELEMETRY_LIMIT);

        List<Trip> allTrips = tripDetector.detect(telemetryPoints);

        if (allTrips.isEmpty()) {
            throw new TripNotFoundException(vehicleId);
        }

        Trip lastTrip = allTrips.stream()
                .max(Comparator.comparing(Trip::getStartedAt))
                .orElseThrow();

        VehicleResponse vehicle = vehicleService.getById(vehicleId);

        return toResponse(lastTrip, vehicle);
    }

    private TripResponse toResponse(Trip trip, VehicleResponse vehicle) {
        List<TripResponse.PointDto> dtoTripPoints = trip.getPoints()
                .stream()
                .map(tripPoint -> TripResponse.PointDto.builder()
                        .ts(tripPoint.getTs())
                        .lat(tripPoint.getLat())
                        .lon(tripPoint.getLon())
                        .speedKph(tripPoint.getSpeedKph())
                        .build())
                .collect(Collectors.toList());

        return TripResponse.builder()
                .vehicle(vehicle)
                .startedAt(trip.getStartedAt())
                .endedAt(trip.getEndedAt())
                .distanceKm(trip.getDistanceKm())
                .avgSpeedKph(trip.getAvgSpeedKph())
                .pointCount(dtoTripPoints.size())
                .points(dtoTripPoints)
                .build();
    }
}
