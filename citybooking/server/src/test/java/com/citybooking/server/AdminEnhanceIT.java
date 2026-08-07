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
 * 节点6：现有 4 块增强（订单分页 / 商家审核拒绝原因 / 订单详情 / 退款拒绝）。
 */
public class AdminEnhanceIT extends BaseIT {

    @Test
    void ordersPaginatedAndDetailMissing() {
        String admin = adminToken();
        Map<?, ?> list = get(BASE + "/admin/orders?page=1&size=10", admin, Map.class);
        assertNotNull(list.get("total"));
        assertEquals(HttpStatus.NOT_FOUND, statusOf(BASE + "/admin/orders/99999999", HttpMethod.GET, null, admin));
    }

    @Test
    void merchantAuditRejectStoresReason() {
        String admin = adminToken();
        Long merchantId = setupMerchant().merchantId;
        assertEquals(HttpStatus.OK,
                statusOf(BASE + "/admin/merchants/" + merchantId + "/audit?approve=false&reason=missing-docs",
                        HttpMethod.POST, null, admin));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rejected = get(BASE + "/admin/merchants?status=REJECTED", admin, List.class);
        assertTrue(rejected != null && !rejected.isEmpty());
        boolean found = rejected.stream()
                .anyMatch(m -> String.valueOf(merchantId).equals(String.valueOf(m.get("id")))
                        && "missing-docs".equals(m.get("rejectReason")));
        assertTrue(found, "被拒商家应携带拒绝原因");
    }

    @Test
    void refundRejectMissingOrder_notFound() {
        assertEquals(HttpStatus.NOT_FOUND,
                statusOf(BASE + "/admin/refunds/99999999/reject", HttpMethod.POST, null, adminToken()));
    }
}
