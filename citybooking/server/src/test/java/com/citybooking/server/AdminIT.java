package com.citybooking.server;

import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.dto.OrderDto.PayResp;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminIT extends BaseIT {

    @Test
    void categoryManageAndRefundApprove() {
        Fixture f = setupMerchant();
        List<?> cats = getList("/admin/categories", f.adminToken);
        assertTrue(cats.size() >= 1, "管理员应能看到类目");

        Long o = createAppointOrder(f);
        post("/orders/" + o + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + o + "/accept", null, f.merchantToken, Void.class);
        post("/orders/" + o + "/start", null, f.merchantToken, Void.class);
        post("/orders/" + o + "/complete", null, f.merchantToken, Void.class);

        post("/admin/refunds/" + o + "/approve", null, f.adminToken, Void.class);
        OrderView v = get("/orders/" + o, f.adminToken, OrderView.class);
        assertEquals("REFUNDED", v.status());
    }

    @Test
    void merchantAuditList() {
        Fixture f = setupMerchant();
        String phone = uniqPhone("m");
        String tk = token(phone, "pwd123", "MERCHANT");
        post("/merchant/onboard", Map.of("name", "待审店铺", "lng", 116.4, "lat", 39.9, "radius", 5000), tk, Void.class);
        List<?> pending = getList("/admin/merchants?status=PENDING", f.adminToken);
        assertTrue(pending.size() >= 1, "待审核商家列表不应为空");
    }
}
