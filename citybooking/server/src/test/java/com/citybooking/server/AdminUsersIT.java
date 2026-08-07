package com.citybooking.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 节点3：用户 / 消费者管理。
 */
public class AdminUsersIT extends BaseIT {

    @Test
    void listAndBanUser() {
        token(uniqPhone("c"), "pwd123", "CONSUMER"); // 基准消费者
        Map<?, ?> list = get(BASE + "/admin/users?page=1&size=10", adminToken(), Map.class);
        assertTrue(((Number) list.get("total")).intValue() >= 1);
        List<?> records = (List<?>) list.get("records");
        Map<?, ?> first = (Map<?, ?>) records.get(0);
        Long id = ((Number) first.get("id")).longValue();

        assertEquals(HttpStatus.OK, statusOf(BASE + "/admin/users/" + id + "/ban", HttpMethod.POST, null, adminToken()));
        assertEquals(HttpStatus.OK, statusOf(BASE + "/admin/users/" + id + "/unban", HttpMethod.POST, null, adminToken()));

        Map<?, ?> orders = get(BASE + "/admin/users/" + id + "/orders?page=1&size=10", adminToken(), Map.class);
        assertNotNull(orders.get("records"));
    }

    @Test
    void banMissingUser_notFound() {
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/users/99999999/ban", HttpMethod.POST, null, adminToken());
        assertEquals(HttpStatus.NOT_FOUND, status);
    }

    @Test
    void userList_forbidden_forConsumer() {
        String consumer = token(uniqPhone("c2"), "pwd123", "CONSUMER");
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/users", HttpMethod.GET, null, consumer);
        assertEquals(HttpStatus.FORBIDDEN, status);
    }
}
