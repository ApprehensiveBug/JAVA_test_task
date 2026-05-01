package com.pingine.fleetpulse.service;

public class TripNotFoundException extends RuntimeException {
    public TripNotFoundException(String vehicleId) {
        super("Trip not found: " + vehicleId);
    }
}
