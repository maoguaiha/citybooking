package com.citybooking.server;

import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.dto.OrderDto.PayResp;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DispatchIT extends BaseIT {

    @Test
    void appointFlow() {
        Fixture f = setupMerchant();
        Long oid = createAppointOrder(f);
        post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + oid + "/accept", null, f.merchantToken, Void.class);
        assertEquals("ACCEPTED", status(oid, f.consumerToken));
        post("/orders/" + oid + "/start", null, f.merchantToken, Void.class);
        assertEquals("SERVICING", status(oid, f.consumerToken));
        post("/orders/" + oid + "/complete", null, f.merchantToken, Void.class);
        assertEquals("COMPLETED", status(oid, f.consumerToken));
    }

    @Test
    void grabFlow() {
        Fixture f = setupMerchant();
        Long oid = createGrabOrder(f);
        post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        post("/orders/" + oid + "/grab", null, f.merchantToken, Void.class);
        assertEquals("ACCEPTED", status(oid, f.consumerToken));
        post("/orders/" + oid + "/start", null, f.merchantToken, Void.class);
        assertEquals("SERVICING", status(oid, f.consumerToken));
        post("/orders/" + oid + "/complete", null, f.merchantToken, Void.class);
        assertEquals("COMPLETED", status(oid, f.consumerToken));
    }

    @Test
    void grabBoardShowsPendingGrab() {
        Fixture f = setupMerchant();
        Long oid = createGrabOrder(f);
        post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        List<?> board = getList("/orders/grab-board", f.merchantToken);
        assertTrue(board.size() >= 1, "抢单看板应至少包含 1 个可抢订单");
    }

    private String status(Long oid, String token) {
        return get("/orders/" + oid, token, OrderView.class).status();
    }
}
