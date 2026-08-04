package com.citybooking.server.payment;

import java.math.BigDecimal;

public interface PaymentChannel {
    String channel(); // mock / wechat / alipay

    PaymentResult pay(Long orderId, BigDecimal amount);

    PaymentResult refund(Long orderId, BigDecimal amount);
}
