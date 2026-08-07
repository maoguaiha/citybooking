package com.citybooking.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MerchantIntegrationIT extends BaseIT {

    @Test
    void publishRequiresApproval() {
        String admin = adminToken();
        String merchant = token(uniqPhone("m"), "pwd123", "MERCHANT");
        Long cat = post("/admin/categories?name=cat", null, admin, Long.class);
        Long mid = post("/merchant/onboard", Map.of(
                "name", "m", "address", "a", "lng", 116.4, "lat", 39.9, "radius", 5000),
                merchant, Long.class);

        // 审核前发布服务应被拒绝
        var before = statusOf("/merchant/services", HttpMethod.POST, Map.of(
                "categoryId", cat, "title", "t", "description", "d", "price", 50.0, "durationMin", 60), merchant);
        assertTrue(before.is4xxClientError());

        post("/admin/merchants/" + mid + "/audit?approve=true", null, admin, Void.class);
        Long sid = post("/merchant/services", Map.of(
                "categoryId", cat, "title", "t", "description", "d", "price", 50.0, "durationMin", 60),
                merchant, Long.class);
        assertNotNull(sid);

        Map<?, ?> profile = getMap("/merchant/profile", merchant);
        assertEquals("APPROVED", profile.get("status"));
    }
}
