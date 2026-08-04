package com.citybooking.server.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.admin.AdminRoles;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.common.SecurityUtil;
import com.citybooking.server.dto.OrderDto.CreateOrderReq;
import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.dto.OrderDto.PayResp;
import com.citybooking.server.geo.GeoService;
import com.citybooking.server.lock.DistributedLock;
import com.citybooking.server.merchant.Merchant;
import com.citybooking.server.merchant.MerchantMapper;
import com.citybooking.server.merchant.MerchantService;
import com.citybooking.server.merchant.ServiceItem;
import com.citybooking.server.merchant.ServiceItemMapper;
import com.citybooking.server.notice.NoticeService;
import com.citybooking.server.payment.PaymentResult;
import com.citybooking.server.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final MerchantService merchantService;
    private final NoticeService noticeService;
    private final GeoService geoService;
    private final MerchantMapper merchantMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final DistributedLock lock;

    @Value("${app.dispatch.grab-timeout-min:30}")
    private long grabTimeoutMin;
    @Value("${app.dispatch.default-radius-m:5000}")
    private double defaultRadius;
    @Value("${app.refund.rate-accepted:0.8}")
    private double refundRate;

    public Order createOrder(CreateOrderReq req, Long uid) {
        ServiceItem service = merchantService.requireService(req.serviceId());
        Order order = new Order();
        order.setConsumerId(uid);
        order.setServiceId(req.serviceId());
        order.setMode(req.mode().toUpperCase());
        order.setAddress(req.address());
        order.setLng(req.lng());
        order.setLat(req.lat());
        order.setAppointmentTime(req.appointmentTime());
        order.setAmount(service.getPrice());
        order.setStatus(OrderStatus.UNPAID.name());
        order.setPayStatus("UNPAID");
        order.setRefundStatus("NONE");
        order.setOrderNo(genOrderNo());

        if ("APPOINT".equals(order.getMode())) {
            if (req.merchantId() == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "指定模式需选择商家");
            }
            Merchant m = merchantService.requireMerchant(req.merchantId());
            if (!"APPROVED".equals(m.getStatus())) {
                throw new BizException(ResultCode.FORBIDDEN, "商家未通过审核");
            }
            order.setMerchantId(m.getId());
            order.setTechnicianId(req.technicianId());
        } else if ("GRAB".equals(order.getMode())) {
            order.setMerchantId(null);
            order.setTechnicianId(null);
        } else {
            throw new BizException(ResultCode.BAD_REQUEST, "不支持的撮合模式");
        }
        orderMapper.insert(order);
        return order;
    }

    public PayResp payOrder(Long orderId, Long uid) {
        Order order = requireOrder(orderId);
        if (!order.getConsumerId().equals(uid)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能支付自己的订单");
        }
        if ("PAID".equals(order.getPayStatus())) {
            return toPayResp(order);
        }
        PaymentResult result = paymentService.pay(orderId, order.getAmount());
        order.setPayStatus("PAID");
        if ("APPOINT".equals(order.getMode())) {
            order.setStatus(OrderStatus.WAIT_ACCEPT.name());
        } else {
            order.setStatus(OrderStatus.PENDING_GRAB.name());
            order.setGrabDeadline(LocalDateTime.now().plusMinutes(grabTimeoutMin));
        }
        orderMapper.updateById(order);
        if ("GRAB".equals(order.getMode()) && order.getLng() != null && order.getLat() != null) {
            broadcastGrab(order);
        }
        noticeService.send(order.getConsumerId(), "ORDER_PAID",
                Map.of("orderId", orderId, "mode", order.getMode()));
        return toPayResp(order);
    }

    public void cancel(Long orderId, Long uid) {
        Order order = requireOrder(orderId);
        if (!order.getConsumerId().equals(uid)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能取消自己的订单");
        }
        lock.withLock("order:" + orderId, 10, () -> {
            Order o = requireOrder(orderId);
            if ("PAID".equals(o.getPayStatus())
                    && (OrderStatus.WAIT_ACCEPT.name().equals(o.getStatus())
                    || OrderStatus.PENDING_GRAB.name().equals(o.getStatus()))) {
                paymentService.refund(orderId, o.getAmount());
                o.setRefundStatus("FULL");
                o.setPayStatus("REFUNDED");
                o.setStatus(OrderStatus.REFUNDED.name());
            } else if (OrderStatus.ACCEPTED.name().equals(o.getStatus())) {
                BigDecimal refund = o.getAmount().multiply(BigDecimal.valueOf(refundRate))
                        .setScale(2, RoundingMode.HALF_UP);
                paymentService.refund(orderId, refund);
                o.setRefundStatus("PARTIAL");
                o.setPayStatus("REFUNDED");
                o.setStatus(OrderStatus.REFUNDED.name());
            } else if (OrderStatus.UNPAID.name().equals(o.getStatus())) {
                o.setStatus(OrderStatus.CANCELLED.name());
            } else {
                throw new BizException(ResultCode.FORBIDDEN, "服务进行中或已完成，需平台仲裁处理");
            }
            orderMapper.updateById(o);
            if (o.getMerchantId() != null) {
                Merchant m = merchantMapper.selectById(o.getMerchantId());
                if (m != null) {
                    noticeService.send(m.getUserId(), "ORDER_CANCELLED", Map.of("orderId", orderId));
                }
            }
            return null;
        });
    }

    public OrderView detail(Long orderId, Long uid, String role) {
        Order order = requireOrder(orderId);
        if (!AdminRoles.ALL.contains(role) && !order.getConsumerId().equals(uid)) {
            if ("MERCHANT".equals(role) || "TECHNICIAN".equals(role)) {
                Merchant m = merchantService.merchantOf(uid);
                if (!order.getMerchantId().equals(m.getId())) {
                    throw new BizException(ResultCode.FORBIDDEN, "无权查看该订单");
                }
            } else {
                throw new BizException(ResultCode.FORBIDDEN, "无权查看该订单");
            }
        }
        return toView(order);
    }

    public PageResult<OrderView> myOrders(Long uid, String role, String status, int page, int size) {
        var q = Wrappers.<Order>lambdaQuery();
        if (AdminRoles.ALL.contains(role)) {
            if (status != null) {
                q.eq(Order::getStatus, status);
            }
        } else if ("MERCHANT".equals(role) || "TECHNICIAN".equals(role)) {
            Merchant m = merchantService.merchantOf(uid);
            q.eq(Order::getMerchantId, m.getId());
            if (status != null) {
                q.eq(Order::getStatus, status);
            }
        } else {
            q.eq(Order::getConsumerId, uid);
            if (status != null) {
                q.eq(Order::getStatus, status);
            }
        }
        q.orderByDesc(Order::getCreatedAt);
        long total = orderMapper.selectCount(q);
        int offset = (page - 1) * size;
        List<Order> pageOrders = orderMapper.selectList(q.last("LIMIT " + size + " OFFSET " + offset));
        List<OrderView> list = pageOrders.stream().map(this::toView).toList();
        return PageResult.of(total, page, size, list);
    }

    private void broadcastGrab(Order order) {
        geoService.nearbyMerchants(order.getLng(), order.getLat(), defaultRadius)
                .forEach(hit -> {
                    Merchant m = merchantMapper.selectById(hit.id());
                    if (m != null && "APPROVED".equals(m.getStatus())) {
                        noticeService.send(m.getUserId(), "GRAB_ORDER",
                                Map.of("orderId", order.getId(), "amount", order.getAmount()));
                    }
                });
    }

    public Order requireOrder(Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return o;
    }

    public PayResp toPayResp(Order order) {
        return new PayResp(order.getId(), "PAID".equals(order.getPayStatus()),
                order.getAmount(), "", "mock");
    }

    public OrderView toView(Order o) {
        ServiceItem s = serviceItemMapper.selectById(o.getServiceId());
        return new OrderView(o.getId(), o.getOrderNo(), o.getConsumerId(), o.getMerchantId(),
                o.getTechnicianId(), o.getServiceId(), s == null ? "" : s.getTitle(),
                o.getMode(), o.getAddress(), o.getLng(), o.getLat(), o.getAppointmentTime(),
                o.getAmount(), o.getStatus(), o.getPayStatus(), o.getRefundStatus(), o.getCreatedAt());
    }

    public List<OrderView> grabBoard(Long uid) {
        var q = Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, OrderStatus.PENDING_GRAB.name())
                .orderByDesc(Order::getCreatedAt);
        return orderMapper.selectList(q).stream().map(this::toView).toList();
    }

    private String genOrderNo() {
        return "NO" + System.currentTimeMillis() + (int) (Math.random() * 9000 + 1000);
    }
}
