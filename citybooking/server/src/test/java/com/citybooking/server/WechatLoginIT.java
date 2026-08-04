package com.citybooking.server;

import com.citybooking.server.dto.AuthDto.AuthResp;
import com.citybooking.server.dto.AuthDto.UserInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WechatLoginIT extends BaseIT {

    @Test
    void wechatLoginAutoRegistersConsumerAndIsIdempotent() {
        AuthResp first = post("/auth/wechat-login", Map.of("code", "wx-it-code-1"), null, AuthResp.class);
        assertNotNull(first.token());
        assertEquals("CONSUMER", first.role());
        assertNotNull(first.userId());

        // 幂等：同一 code 返回同一用户
        AuthResp again = post("/auth/wechat-login", Map.of("code", "wx-it-code-1"), null, AuthResp.class);
        assertEquals(first.userId(), again.userId());

        // 不同 code 生成不同用户，且 token 可访问 /auth/me
        AuthResp other = post("/auth/wechat-login", Map.of("code", "wx-it-code-2"), null, AuthResp.class);
        assertNotEquals(first.userId(), other.userId());

        UserInfo me = get("/auth/me", other.token(), UserInfo.class);
        assertEquals(other.userId(), me.id());
        assertEquals("CONSUMER", me.role());
    }
}
