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
 * 节点1：管理员体系（初始化 + 角色分级）。
 * 用 @SpringBootTest(RANDOM_PORT) + TestRestTemplate 实测端点，兼顾单测与冒烟。
 */
public class AdminAccountIT extends BaseIT {

    @Test
    void selfRegisterAdmin_isForbidden() {
        // 自助注册 ADMIN 角色应被拒绝（400）
        HttpStatus status = (HttpStatus) statusOf(BASE + "/auth/register", HttpMethod.POST, Map.of(
                "phone", uniqPhone("x"), "password", "pwd123", "nickname", "hack", "role", "ADMIN"), null);
        assertEquals(HttpStatus.BAD_REQUEST, status);
    }

    @Test
    void seedSuperAdmin_canAccessOperations() {
        String adminToken = adminToken();
        assertNotNull(adminToken);
        // 超管可访问运营端点（商户列表）
        HttpStatus status = (HttpStatus) statusOf(BASE + "/admin/merchants", HttpMethod.GET, null, adminToken);
        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void superAdmin_canCreateAndListSubAdmin() {
        String superToken = adminToken();
        String phone = uniqPhone("sub");
        Map<?, ?> created = post(BASE + "/admin/admins", Map.of(
                "phone", phone, "password", "sub123456", "nickname", "运营小妹"), superToken, Map.class);
        assertNotNull(created);
        // 列出管理员应包含新创建者
        List<?> admins = getList(BASE + "/admin/admins", superToken);
        assertTrue(admins.stream().anyMatch(a -> phone.equals(((Map<?, ?>) a).get("phone"))));
    }

    @Test
    void subAdmin_cannotManageAdmins() {
        String superToken = adminToken();
        String phone = uniqPhone("op");
        post(BASE + "/admin/admins", Map.of("phone", phone, "password", "op123456"), superToken, Map.class);
        // 子管理员已存在，token() 会先尝试注册(被拒并忽略)再登录，正好取回其 token
        String opToken = token(phone, "op123456", "ADMIN");
        // 子管理员可访问运营端点
        assertEquals(HttpStatus.OK, statusOf(BASE + "/admin/merchants", HttpMethod.GET, null, opToken));
        // 子管理员访问管理员管理端点 -> 403
        assertEquals(HttpStatus.FORBIDDEN, statusOf(BASE + "/admin/admins", HttpMethod.GET, null, opToken));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(BASE + "/admin/admins", HttpMethod.POST,
                Map.of("phone", uniqPhone("z"), "password", "z123456"), opToken));
    }

    @Test
    void consumer_cannotAccessAdmin() {
        String consumerToken = token(uniqPhone("c"), "pwd123", "CONSUMER");
        assertEquals(HttpStatus.FORBIDDEN, statusOf(BASE + "/admin/merchants", HttpMethod.GET, null, consumerToken));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(BASE + "/admin/admins", HttpMethod.GET, null, consumerToken));
    }
}
