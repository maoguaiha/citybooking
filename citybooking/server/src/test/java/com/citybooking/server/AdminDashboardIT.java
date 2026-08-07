package com.citybooking.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 节点2：平台数据看板。
 */
public class AdminDashboardIT extends BaseIT {

    @Test
    void dashboard_returnsOverview() {
        token(uniqPhone("c"), "pwd123", "CONSUMER"); // 创建一名消费者作为基准数据
        Map<?, ?> body = get(BASE + "/admin/dashboard", adminToken(), Map.class);
        assertNotNull(body);
        // 至少含刚创建的消费者，且核心指标存在
        assertTrue(((Number) body.get("totalUsers")).intValue() >= 1);
        assertNotNull(body.get("todayGmv"));
        assertNotNull(body.get("pendingMerchants"));
        assertNotNull(body.get("totalServices"));
    }

    @Test
    void dashboard_forbidden_forConsumer() {
        String consumer = token(uniqPhone("c"), "pwd123", "CONSUMER");
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/dashboard", HttpMethod.GET, null, consumer);
        assertEquals(HttpStatus.FORBIDDEN, status);
    }
}
