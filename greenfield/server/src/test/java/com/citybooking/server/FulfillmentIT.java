package com.citybooking.server;

import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.dto.OrderDto.PayResp;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FulfillmentIT extends BaseIT {

    @Test
    void reviewAndRefundTiers() {
        Fixture f = setupMerchant();

        // 未接单取消 -> 全额退款
        Long o1 = createAppointOrder(f);
        post("/orders/" + o1 + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + o1 + "/cancel", null, f.consumerToken, Void.class);
        OrderView v1 = get("/orders/" + o1, f.consumerToken, OrderView.class);
        assertEquals("REFUNDED", v1.status());
        assertEquals("FULL", v1.refundStatus());

        // 已接单取消 -> 部分退款
        Long o2 = createAppointOrder(f);
        post("/orders/" + o2 + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + o2 + "/accept", null, f.merchantToken, Void.class);
        post("/orders/" + o2 + "/cancel", null, f.consumerToken, Void.class);
        OrderView v2 = get("/orders/" + o2, f.consumerToken, OrderView.class);
        assertEquals("REFUNDED", v2.status());
        assertEquals("PARTIAL", v2.refundStatus());

        // 已完成 -> 评价；且不可再取消
        Long o3 = createAppointOrder(f);
        post("/orders/" + o3 + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + o3 + "/accept", null, f.merchantToken, Void.class);
        post("/orders/" + o3 + "/start", null, f.merchantToken, Void.class);
        post("/orders/" + o3 + "/complete", null, f.merchantToken, Void.class);
        post("/orders/" + o3 + "/review", Map.of("score", 5, "comment", "服务很好"),
                f.consumerToken, Void.class);

        var s = statusOf("/orders/" + o3 + "/cancel", HttpMethod.POST, null, f.consumerToken);
        assertTrue(s.is4xxClientError());
    }
}
