package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Splits a stream of telemetry points into completed trips.
 * A trip starts on ignition=true and ends on the next ignition=false.
 */
@Component
public class TripDetector {

    public List<Trip> detect(List<TelemetryPoint> points) {

        if (points == null || points.isEmpty()) {
            return List.of();
        }

        Map<String, List<TelemetryPoint>> groupedById =
                points.stream()
                        .collect(Collectors.groupingBy(TelemetryPoint::getVehicleId));
        // получили map из машин, у каждой есть список её точек телеметрии -- сгруппировали по машине (признак)

        List<Trip> trips = new ArrayList<>();
        // все трипы -- трипы каждой машины подряд без смешиваний

        for (String vehicleId : groupedById.keySet()) {
            List<TelemetryPoint> vehiclePoints = groupedById.get(vehicleId);

            vehiclePoints.sort(Comparator.comparing(TelemetryPoint::getTs));

            trips.addAll(buildTripsForVehicle(vehicleId, vehiclePoints));
        }
        return trips;
    }

    private List<Trip> buildTripsForVehicle(String vehicleId, List<TelemetryPoint> points) {
        List<Trip> result = new ArrayList<>();

        TelemetryPoint start = null;
        List<Trip.TripPoint> tripPoints = new ArrayList<>();

        for (TelemetryPoint point : points) {

            if (start == null) {

                if (point.isIgnition()) {
                    start = point;

                    tripPoints = new ArrayList<>();
                    tripPoints.add(toTripPoint(point));
                }

            } else {

                Trip.TripPoint tp = toTripPoint(point);

                if (!tripPoints.get(tripPoints.size() - 1).equals(tp)) {
                    tripPoints.add(tp);
                }

                if (!point.isIgnition()) {

                    Trip trip = createTrip(vehicleId, start, point, tripPoints);

                    result.add(trip);

                    start = null;
                    tripPoints = new ArrayList<>();
                }
            }
        }
        return result;
    }

    private Trip createTrip(
            String vehicleId,
            TelemetryPoint start,
            TelemetryPoint end,
            List<Trip.TripPoint> points
    ) {

        return Trip.builder()
                .vehicleId(vehicleId)
                .startedAt(start.getTs().toInstant(ZoneOffset.UTC))
                .endedAt(end.getTs().toInstant(ZoneOffset.UTC))
                .distanceKm(calculateDistance(points))
                .avgSpeedKph(calculateAvgSpeed(points))
                .points(points)
                .build();
    }

    private Trip.TripPoint toTripPoint(TelemetryPoint p) {
        return Trip.TripPoint.builder()
                .ts(p.getTs().toInstant(ZoneOffset.UTC))
                .lat(p.getLat())
                .lon(p.getLon())
                .speedKph(p.getSpeed())
                .build();
    }

    private double calculateDistance(List<Trip.TripPoint> points) {

        double total = 0;

        for (int i = 1; i < points.size(); i++) {

            Trip.TripPoint prev = points.get(i - 1);
            Trip.TripPoint curr = points.get(i);

            total += GeoDistance.haversineKm(
                    prev.getLat(),
                    prev.getLon(),
                    curr.getLat(),
                    curr.getLon()
            );
        }

        return total;
    }

    private double calculateAvgSpeed(List<Trip.TripPoint> points) {

        return points.stream()
                .mapToDouble(Trip.TripPoint::getSpeedKph)
                .average()
                .orElse(0);
    }
}
