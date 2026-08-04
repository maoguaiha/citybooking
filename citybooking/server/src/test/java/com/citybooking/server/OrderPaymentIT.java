package com.citybooking.server;

import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.dto.OrderDto.PayResp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderPaymentIT extends BaseIT {

    @Test
    void payAndIdempotent() {
        Fixture f = setupMerchant();
        Long oid = createAppointOrder(f);
        PayResp pay = post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        assertTrue(pay.paid());

        OrderView ov = get("/orders/" + oid, f.consumerToken, OrderView.class);
        assertEquals("WAIT_ACCEPT", ov.status());

        PayResp pay2 = post("/orders/" + oid + "/pay", null, f.consumerToken, PayResp.class);
        assertTrue(pay2.paid());
    }
}
