package com.citybooking.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthIntegrationIT extends BaseIT {

    @Test
    void registerLoginAndMe() {
        String phone = uniqPhone("u");
        String token = token(phone, "pwd123", "CONSUMER");
        assertNotNull(token);
        Map<?, ?> me = getMap("/auth/me", token);
        assertEquals("CONSUMER", me.get("role"));
    }

    @Test
    void loginWrongPasswordFails() {
        String phone = uniqPhone("u");
        token(phone, "pwd123", "CONSUMER");
        var s = statusOf("/auth/login", HttpMethod.POST,
                Map.of("phone", phone, "password", "wrong"), null);
        assertTrue(s.is4xxClientError());
    }

    @Test
    void duplicatePhoneRejected() {
        String phone = uniqPhone("u");
        token(phone, "pwd123", "CONSUMER");
        var s = statusOf("/auth/register", HttpMethod.POST,
                Map.of("phone", phone, "password", "pwd123", "nickname", "x", "role", "CONSUMER"), null);
        assertTrue(s.is4xxClientError());
    }
}
