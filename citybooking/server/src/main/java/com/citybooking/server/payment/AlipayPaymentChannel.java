package com.citybooking.server.payment;

import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付宝通道骨架（生产接入点）。当前未配置应用 ID，调用即提示接入。
 */
@Component
public class AlipayPaymentChannel implements PaymentChannel {

    @Override
    public String channel() {
        return "alipay";
    }

    @Override
    public PaymentResult pay(Long orderId, BigDecimal amount) {
        throw new BizException(ResultCode.INTERNAL, "支付宝通道未接入，请配置应用 APP_ID");
    }

    @Override
    public PaymentResult refund(Long orderId, BigDecimal amount) {
        throw new BizException(ResultCode.INTERNAL, "支付宝通道未接入，请配置应用 APP_ID");
    }
}
