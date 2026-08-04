package com.citybooking.server;

import com.citybooking.server.dto.MerchantDto.ServiceView;
import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.dto.OrderDto.PayResp;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对 N0-N9 已覆盖用例之外的接口进行补充功能测试，确保核心链路无遗漏：
 * 服务详情、我的服务、添加技师、我的订单（消费者/商家）、管理员列表、退款阶段边界等。
 */
public class ExtraCoverageIT extends BaseIT {

    /** 在列表中按 id 字段查找是否存在目标 id（兼容 JSON 反序列化的数值类型）。 */
    private boolean listContainsId(List<?> list, long id) {
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                Object v = m.get("id");
                if (v instanceof Number n && n.longValue() == id) return true;
            }
        }
        return false;
    }

    @Test
    void serviceDetailReturnsPublishedService() {
        Fixture f = setupMerchant();
        ServiceView v = get("/services/" + f.serviceId, f.consumerToken, ServiceView.class);
        assertEquals("保洁3小时", v.title());
        assertEquals("测试商家", v.merchantName());
        assertTrue(v.price() != null && v.price().signum() > 0);
    }

    @Test
    void serviceDetailNotFoundForUnknownId() {
        HttpStatusCode s = statusOf("/services/99999999", HttpMethod.GET, null, null);
        assertTrue(s.is4xxClientError());
    }

    @Test
    void myServicesListsOwnService() {
        Fixture f = setupMerchant();
        List<?> list = getList("/merchant/services", f.merchantToken);
        assertFalse(list.isEmpty());
        assertTrue(listContainsId(list, f.serviceId));
    }

    @Test
    void addTechnicianReturnsId() {
        Fixture f = setupMerchant();
        Long tid = post("/merchant/technicians",
                Map.of("name", "小李", "skill", "水电维修", "lng", 116.40, "lat", 39.90, "merchantId", f.merchantId),
                f.merchantToken, Long.class);
        assertNotNull(tid);
        assertTrue(tid > 0);
    }

    @Test
    void myOrdersListsConsumerOrderAndStatusFilter() {
        Fixture f = setupMerchant();
        Long oid = createAppointOrder(f);
        Map<String, Object> m = getMap("/orders?page=1&size=20", f.consumerToken);
        List<?> records = (List<?>) m.get("records");
        assertFalse(records.isEmpty());
        assertTrue(listContainsId(records, oid));

        Map<String, Object> m2 = getMap("/orders?status=UNPAID", f.consumerToken);
        assertTrue(listContainsId((List<?>) m2.get("records"), oid));
    }

    @Test
    void merchantSeesAssignedOrders() {
        Fixture f = setupMerchant();
        Long oid = createAppointOrder(f);
        post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + oid + "/accept", null, f.merchantToken, Void.class);
        Map<String, Object> m = getMap("/orders", f.merchantToken);
        assertTrue(listContainsId((List<?>) m.get("records"), oid));
    }

    @Test
    void adminMerchantsNoFilterReturnsApproved() {
        Fixture f = setupMerchant();
        List<?> list = getList("/admin/merchants", f.adminToken);
        assertFalse(list.isEmpty());
        assertTrue(listContainsId(list, f.merchantId));
    }

    @Test
    void adminOrdersListsAllOrders() {
        Fixture f = setupMerchant();
        Long oid = createAppointOrder(f);
        post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        List<?> list = getList("/admin/orders", f.adminToken);
        assertTrue(listContainsId(list, oid));
    }

    @Test
    void cancelUnpaidOrderBecomesCancelled() {
        Fixture f = setupMerchant();
        Long oid = createAppointOrder(f);
        post("/orders/" + oid + "/cancel", null, f.consumerToken, Void.class);
        OrderView v = get("/orders/" + oid, f.consumerToken, OrderView.class);
        assertEquals("CANCELLED", v.status());
    }

    @Test
    void cancelAfterStartRejected() {
        Fixture f = setupMerchant();
        Long oid = createAppointOrder(f);
        post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + oid + "/accept", null, f.merchantToken, Void.class);
        post("/orders/" + oid + "/start", null, f.merchantToken, Void.class);
        HttpStatusCode s = statusOf("/orders/" + oid + "/cancel", HttpMethod.POST, null, f.consumerToken);
        assertTrue(s.is4xxClientError());
    }

    @Test
    void grabAlreadyGrabbedRejected() {
        Fixture f = setupMerchant();
        String m2 = token(uniqPhone("m2g"), "pwd123", "MERCHANT");
        Long m2Id = post("/merchant/onboard",
                Map.of("name", "抢单商家2", "address", "北京", "lng", 116.41, "lat", 39.91, "radius", 5000),
                m2, Long.class);
        post("/admin/merchants/" + m2Id + "/audit?approve=true", null, f.adminToken, Void.class);

        Long goid = createGrabOrder(f);
        post("/orders/" + goid + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + goid + "/grab", null, f.merchantToken, Void.class);
        HttpStatusCode s = statusOf("/orders/" + goid + "/grab", HttpMethod.POST, null, m2);
        assertTrue(s.is4xxClientError());
    }

    @Test
    void categoryCreateAndPublicListing() {
        Fixture f = setupMerchant();
        Long cat = post("/admin/categories?name=临时类目X", null, f.adminToken, Long.class);
        assertNotNull(cat);
        assertTrue(cat > 0);
        List<?> list = getList("/services/categories", f.consumerToken);
        assertTrue(listContainsId(list, cat));
    }
}
