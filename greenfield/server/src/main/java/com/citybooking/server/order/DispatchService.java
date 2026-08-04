package com.citybooking.server.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.common.SecurityUtil;
import com.citybooking.server.dto.OrderDto.GrabReq;
import com.citybooking.server.lock.DistributedLock;
import com.citybooking.server.merchant.Merchant;
import com.citybooking.server.merchant.MerchantService;
import com.citybooking.server.notice.NoticeService;
import com.citybooking.server.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final OrderMapper orderMapper;
    private final GrabRecordMapper grabRecordMapper;
    private final MerchantService merchantService;
    private final NoticeService noticeService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final DistributedLock lock;

    public void acceptOrder(Long orderId, Long uid) {
        Order order = orderService.requireOrder(orderId);
        if (!"APPOINT".equals(order.getMode())) {
            throw new BizException(ResultCode.BAD_REQUEST, "该订单非指定模式");
        }
        if (!OrderStatus.WAIT_ACCEPT.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "订单状态不可接单");
        }
        Merchant merchant = merchantService.merchantOf(uid);
        if (!order.getMerchantId().equals(merchant.getId())) {
            throw new BizException(ResultCode.FORBIDDEN, "非该商家的订单");
        }
        lock.withLock("order:" + orderId, 10, () -> {
            Order o = orderService.requireOrder(orderId);
            if (!OrderStatus.WAIT_ACCEPT.name().equals(o.getStatus())) {
                throw new BizException(ResultCode.CONFLICT, "来晚一步，订单已被处理");
            }
            o.setStatus(OrderStatus.ACCEPTED.name());
            orderMapper.updateById(o);
            noticeService.send(o.getConsumerId(), "ORDER_ACCEPTED", Map.of("orderId", orderId));
            return null;
        });
    }

    public void grabOrder(Long orderId, Long uid, GrabReq req) {
        Order order = orderService.requireOrder(orderId);
        if (!"GRAB".equals(order.getMode())) {
            throw new BizException(ResultCode.BAD_REQUEST, "该订单非抢单模式");
        }
        if (!OrderStatus.PENDING_GRAB.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "订单不可抢（已关闭或被抢）");
        }
        Merchant merchant = merchantService.merchantOf(uid);
        lock.withLock("order:" + orderId, 10, () -> {
            Order o = orderService.requireOrder(orderId);
            if (!OrderStatus.PENDING_GRAB.name().equals(o.getStatus())) {
                throw new BizException(ResultCode.CONFLICT, "来晚一步，已被其他服务者抢走");
            }
            o.setMerchantId(merchant.getId());
            o.setTechnicianId(req == null ? null : req.technicianId());
            o.setStatus(OrderStatus.ACCEPTED.name());
            orderMapper.updateById(o);
            GrabRecord record = new GrabRecord();
            record.setOrderId(orderId);
            record.setMerchantId(merchant.getId());
            record.setTechnicianId(req == null ? null : req.technicianId());
            record.setStatus("GRABBED");
            grabRecordMapper.insert(record);
            noticeService.send(o.getConsumerId(), "ORDER_GRABBED",
                    Map.of("orderId", orderId, "merchantId", merchant.getId()));
            return null;
        });
    }

    public void startService(Long orderId, Long uid) {
        transition(orderId, uid, OrderStatus.ACCEPTED, OrderStatus.SERVICING, "ORDER_SERVICING");
    }

    public void completeService(Long orderId, Long uid) {
        transition(orderId, uid, OrderStatus.SERVICING, OrderStatus.COMPLETED, "ORDER_COMPLETED");
    }

    private void transition(Long orderId, Long uid, OrderStatus from, OrderStatus to, String notice) {
        Order order = orderService.requireOrder(orderId);
        Merchant merchant = merchantService.merchantOf(uid);
        if (!order.getMerchantId().equals(merchant.getId())) {
            throw new BizException(ResultCode.FORBIDDEN, "非该商家的订单");
        }
        if (!from.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "订单状态不可操作");
        }
        lock.withLock("order:" + orderId, 10, () -> {
            Order o = orderService.requireOrder(orderId);
            if (!from.name().equals(o.getStatus())) {
                throw new BizException(ResultCode.CONFLICT, "订单状态已变更");
            }
            o.setStatus(to.name());
            orderMapper.updateById(o);
            noticeService.send(o.getConsumerId(), notice, Map.of("orderId", orderId));
            return null;
        });
    }

    public void expireGrabOrders() {
        List<Order> expired = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getMode, "GRAB")
                .eq(Order::getStatus, OrderStatus.PENDING_GRAB.name())
                .le(Order::getGrabDeadline, LocalDateTime.now()));
        for (Order order : expired) {
            lock.withLock("order:" + order.getId(), 10, () -> {
                Order o = orderService.requireOrder(order.getId());
                if (!OrderStatus.PENDING_GRAB.name().equals(o.getStatus())) {
                    return null;
                }
                paymentService.refund(o.getId(), o.getAmount());
                o.setRefundStatus("FULL");
                o.setPayStatus("REFUNDED");
                o.setStatus(OrderStatus.CLOSED.name());
                orderMapper.updateById(o);
                noticeService.send(o.getConsumerId(), "ORDER_EXPIRED",
                        Map.of("orderId", o.getId(), "reason", "无人接单，已全额退款"));
                return null;
            });
        }
    }
}
