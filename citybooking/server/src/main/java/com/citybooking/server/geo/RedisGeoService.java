package com.citybooking.server.geo;

import org.springframework.context.annotation.Profile;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
public class RedisGeoService implements GeoService {

    private static final String MK = "geo:merchant";
    private static final String TK = "geo:technician";

    private final StringRedisTemplate redis;

    public RedisGeoService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void addMerchant(Long id, double lng, double lat) {
        redis.opsForGeo().add(MK, new Point(lng, lat), String.valueOf(id));
    }

    @Override
    public void addTechnician(Long id, double lng, double lat) {
        redis.opsForGeo().add(TK, new Point(lng, lat), String.valueOf(id));
    }

    @Override
    public void removeMerchant(Long id) {
        redis.opsForGeo().remove(MK, String.valueOf(id));
    }

    @Override
    public void removeTechnician(Long id) {
        redis.opsForGeo().remove(TK, String.valueOf(id));
    }

    @Override
    public List<GeoHit> nearbyMerchants(double lng, double lat, double radiusM) {
        return within(MK, lng, lat, radiusM);
    }

    @Override
    public List<GeoHit> nearbyTechnicians(double lng, double lat, double radiusM) {
        return within(TK, lng, lat, radiusM);
    }

    private List<GeoHit> within(String key, double lng, double lat, double radiusM) {
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusM));
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redis.opsForGeo().radius(key, circle, RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance());
        List<GeoHit> hits = new ArrayList<>();
        if (results != null) {
            for (var r : results) {
                Long id = Long.valueOf(r.getContent().getName());
                double d = r.getDistance() == null ? 0 : r.getDistance().getValue();
                hits.add(new GeoHit(id, d));
            }
        }
        return hits;
    }
}
