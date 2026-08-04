package com.citybooking.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceSearchIT extends BaseIT {

    @Test
    void nearbySearchReturnsService() {
        Fixture f = setupMerchant();
        Map<?, ?> res = getMap("/services?lng=116.40&lat=39.90&radius=10000", null);
        Object total = res.get("total");
        assertTrue(total != null && ((Number) total).intValue() >= 1, "附近应至少返回 1 个服务");
    }

    @Test
    void publicCategoriesListed() {
        List<?> cats = getList("/services/categories", null);
        assertTrue(cats.size() >= 6, "应返回已初始化的分类");
    }
}
