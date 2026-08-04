package com.citybooking.server.payment;

import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 微信支付通道骨架（生产接入点）。当前未配置商户号/证书，调用即提示接入。
 */
@Component
public class WechatPaymentChannel implements PaymentChannel {

    @Override
    public String channel() {
        return "wechat";
    }

    @Override
    public PaymentResult pay(Long orderId, BigDecimal amount) {
        throw new BizException(ResultCode.INTERNAL, "微信支付通道未接入，请配置商户号与 API 证书");
    }

    @Override
    public PaymentResult refund(Long orderId, BigDecimal amount) {
        throw new BizException(ResultCode.INTERNAL, "微信支付通道未接入，请配置商户号与 API 证书");
    }
}
