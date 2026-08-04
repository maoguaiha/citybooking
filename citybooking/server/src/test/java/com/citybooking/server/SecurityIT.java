package com.citybooking.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 鉴权与 RBAC 边界用例：401 未登录、403 越权访问。
 * 这些用例只验证 HTTP 状态，不依赖业务数据，确保任何未授权访问都正确被拦截。
 */
public class SecurityIT extends BaseIT {

    @Test
    void unauthenticatedMeRejected() {
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf("/auth/me", HttpMethod.GET, null, null));
    }

    @Test
    void unauthenticatedCreateOrderRejected() {
        // 未登录访问受保护接口必须被拒绝（401/403 均表示拦截生效）
        assertTrue(statusOf("/orders", HttpMethod.POST, null, null).is4xxClientError());
    }

    @Test
    void unauthenticatedPublicServiceStillOpen() {
        // 公开搜索接口不应被拦截
        assertEquals(HttpStatus.OK, statusOf("/services?page=1&size=10", HttpMethod.GET, null, null));
    }

    @Test
    void consumerCannotOnboard() {
        String consumer = token(uniqPhone("csec"), "pwd123", "CONSUMER");
        Map<String, Object> body = Map.of("name", "x", "address", "a", "lng", 116.0, "lat", 39.0, "radius", 5000);
        assertEquals(HttpStatus.FORBIDDEN, statusOf("/merchant/onboard", HttpMethod.POST, body, consumer));
    }

    @Test
    void consumerCannotAccessGrabBoard() {
        String consumer = token(uniqPhone("csec2"), "pwd123", "CONSUMER");
        assertEquals(HttpStatus.FORBIDDEN, statusOf("/orders/grab-board", HttpMethod.GET, null, consumer));
    }

    @Test
    void consumerCannotAccessAdminMerchants() {
        String consumer = token(uniqPhone("csec3"), "pwd123", "CONSUMER");
        assertEquals(HttpStatus.FORBIDDEN, statusOf("/admin/merchants", HttpMethod.GET, null, consumer));
    }

    @Test
    void consumerCannotReviewAsMerchant() {
        String consumer = token(uniqPhone("csec4"), "pwd123", "CONSUMER");
        assertEquals(HttpStatus.FORBIDDEN, statusOf("/merchant/services", HttpMethod.GET, null, consumer));
    }

    @Test
    void merchantCannotCreateCategory() {
        String merchant = token(uniqPhone("msec"), "pwd123", "MERCHANT");
        assertEquals(HttpStatus.FORBIDDEN, statusOf("/admin/categories?name=禁止", HttpMethod.POST, null, merchant));
    }

    @Test
    void technicianCannotAuditMerchant() {
        String tech = token(uniqPhone("tsec"), "pwd123", "TECHNICIAN");
        assertEquals(HttpStatus.FORBIDDEN, statusOf("/admin/merchants/1/audit?approve=true", HttpMethod.POST, null, tech));
    }
}
