package com.citybooking.server.payment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.order.Payment;
import com.citybooking.server.order.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final List<PaymentChannel> channels;

    @Value("${app.pay.channel:mock}")
    private String activeChannel;

    public PaymentResult pay(Long orderId, BigDecimal amount) {
        Payment paid = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, orderId).eq(Payment::getStatus, "PAID"));
        if (paid != null) {
            return new PaymentResult(paid.getTradeNo(), true); // 幂等
        }
        PaymentChannel channel = channels.stream()
                .filter(c -> c.channel().equals(activeChannel))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.INTERNAL, "支付通道未配置"));
        PaymentResult result = channel.pay(orderId, amount);

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setChannel(channel.channel());
        payment.setAmount(amount);
        payment.setTradeNo(result.tradeNo());
        payment.setStatus(result.success() ? "PAID" : "FAILED");
        payment.setPaidAt(result.success() ? LocalDateTime.now() : null);
        paymentMapper.insert(payment);
        if (!result.success()) {
            throw new BizException(ResultCode.INTERNAL, "支付失败");
        }
        return result;
    }

    public PaymentResult refund(Long orderId, BigDecimal amount) {
        PaymentChannel channel = channels.stream()
                .filter(c -> c.channel().equals(activeChannel))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.INTERNAL, "支付通道未配置"));
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, orderId));
        if (payment == null) {
            throw new BizException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        if ("REFUNDED".equals(payment.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "订单已退款");
        }
        PaymentResult result = channel.refund(orderId, amount);
        payment.setStatus("REFUNDED");
        paymentMapper.updateById(payment);
        return result;
    }

    public Payment requirePaid(Long orderId) {
        Payment p = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, orderId).eq(Payment::getStatus, "PAID"));
        if (p == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "订单未支付");
        }
        return p;
    }
}
