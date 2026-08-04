package com.citybooking.server.geo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!prod")
public class InMemoryGeoService implements GeoService {

    private final Map<Long, double[]> merchants = new ConcurrentHashMap<>();
    private final Map<Long, double[]> technicians = new ConcurrentHashMap<>();

    @Override
    public void addMerchant(Long id, double lng, double lat) {
        merchants.put(id, new double[]{lng, lat});
    }

    @Override
    public void addTechnician(Long id, double lng, double lat) {
        technicians.put(id, new double[]{lng, lat});
    }

    @Override
    public void removeMerchant(Long id) {
        merchants.remove(id);
    }

    @Override
    public void removeTechnician(Long id) {
        technicians.remove(id);
    }

    @Override
    public List<GeoHit> nearbyMerchants(double lng, double lat, double radiusM) {
        return within(merchants, lng, lat, radiusM);
    }

    @Override
    public List<GeoHit> nearbyTechnicians(double lng, double lat, double radiusM) {
        return within(technicians, lng, lat, radiusM);
    }

    private List<GeoHit> within(Map<Long, double[]> points, double lng, double lat, double radiusM) {
        List<GeoHit> hits = new ArrayList<>();
        for (Map.Entry<Long, double[]> e : points.entrySet()) {
            double d = haversine(lng, lat, e.getValue()[0], e.getValue()[1]);
            if (d <= radiusM) {
                hits.add(new GeoHit(e.getKey(), d));
            }
        }
        hits.sort((a, b) -> Double.compare(a.distanceM(), b.distanceM()));
        return hits;
    }

    private double haversine(double lng1, double lat1, double lng2, double lat2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
