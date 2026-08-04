package com.citybooking.server.geo;

import java.util.List;

/**
 * LBS 抽象。开发/测试用内存 Haversine 实现，生产切换 Redis GEO（见 GeoConfig）。
 */
public interface GeoService {

    void addMerchant(Long id, double lng, double lat);

    void addTechnician(Long id, double lng, double lat);

    void removeMerchant(Long id);

    void removeTechnician(Long id);

    List<GeoHit> nearbyMerchants(double lng, double lat, double radiusM);

    List<GeoHit> nearbyTechnicians(double lng, double lat, double radiusM);
}
