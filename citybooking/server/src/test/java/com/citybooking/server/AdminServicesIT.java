package com.citybooking.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 节点5：服务内容治理。
 */
public class AdminServicesIT extends BaseIT {

    private Long firstServiceId(Map<?, ?> list) {
        List<?> records = (List<?>) list.get("records");
        assertTrue(records != null && !records.isEmpty());
        return ((Number) ((Map<?, ?>) records.get(0)).get("id")).longValue();
    }

    @Test
    void listAndOfflineRestoreService() {
        String admin = adminToken();
        setupMerchant(); // 已建好「测试类目」+ 审核通过商户 + 一个服务

        Map<?, ?> list = get(BASE + "/admin/services?page=1&size=10", admin, Map.class);
        assertTrue(((Number) list.get("total")).intValue() >= 1);
        Long serviceId = firstServiceId(list);

        assertEquals(HttpStatus.OK, statusOf(BASE + "/admin/services/" + serviceId + "/offline", HttpMethod.POST, null, admin));
        assertEquals(HttpStatus.OK, statusOf(BASE + "/admin/services/" + serviceId + "/restore", HttpMethod.POST, null, admin));
    }

    @Test
    void offlineMissingService_notFound() {
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/services/99999999/offline", HttpMethod.POST, null, adminToken());
        assertEquals(HttpStatus.NOT_FOUND, status);
    }

    @Test
    void serviceList_forbidden_forConsumer() {
        String consumer = token(uniqPhone("c"), "pwd123", "CONSUMER");
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/services", HttpMethod.GET, null, consumer);
        assertEquals(HttpStatus.FORBIDDEN, status);
    }
}
