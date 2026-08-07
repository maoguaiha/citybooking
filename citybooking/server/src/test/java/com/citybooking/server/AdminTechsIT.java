package com.citybooking.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 节点4：技师管理。
 */
public class AdminTechsIT extends BaseIT {

    private Long addTechnician(String token) {
        Map<String, Object> body = Map.of(
                "name", "技师张三",
                "skill", "家政保洁",
                "lng", 121.47,
                "lat", 31.23);
        return post(BASE + "/merchant/technicians", body, token, Long.class);
    }

    @Test
    void listAndEnableDisableTechnician() {
        String admin = adminToken();
        String merchant = setupMerchant().merchantToken;
        Long techId = addTechnician(merchant);

        Map<?, ?> list = get(BASE + "/admin/technicians?page=1&size=10", admin, Map.class);
        assertTrue(((Number) list.get("total")).intValue() >= 1);

        assertEquals(HttpStatus.OK, statusOf(BASE + "/admin/technicians/" + techId + "/disable", HttpMethod.POST, null, admin));
        assertEquals(HttpStatus.OK, statusOf(BASE + "/admin/technicians/" + techId + "/enable", HttpMethod.POST, null, admin));
    }

    @Test
    void disableMissingTechnician_notFound() {
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/technicians/99999999/disable", HttpMethod.POST, null, adminToken());
        assertEquals(HttpStatus.NOT_FOUND, status);
    }

    @Test
    void techList_forbidden_forConsumer() {
        String consumer = token(uniqPhone("c"), "pwd123", "CONSUMER");
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/technicians", HttpMethod.GET, null, consumer);
        assertEquals(HttpStatus.FORBIDDEN, status);
    }
}
