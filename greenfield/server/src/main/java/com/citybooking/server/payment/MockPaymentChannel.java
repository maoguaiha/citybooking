package com.citybooking.server.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentChannel implements PaymentChannel {

    @Override
    public String channel() {
        return "mock";
    }

    @Override
    public PaymentResult pay(Long orderId, BigDecimal amount) {
        return new PaymentResult("MOCK" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), true);
    }

    @Override
    public PaymentResult refund(Long orderId, BigDecimal amount) {
        return new PaymentResult("MOCKR" + UUID.randomUUID().toString().replace("-", "").substring(0, 18), true);
    }
}
