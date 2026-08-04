package com.citybooking.server.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.merchant.Category;
import com.citybooking.server.merchant.CategoryMapper;
import com.citybooking.server.merchant.Merchant;
import com.citybooking.server.merchant.MerchantMapper;
import com.citybooking.server.merchant.MerchantService;
import com.citybooking.server.dto.MerchantDto.MerchantView;
import com.citybooking.server.notice.NoticeService;
import com.citybooking.server.order.Order;
import com.citybooking.server.order.OrderMapper;
import com.citybooking.server.order.OrderService;
import com.citybooking.server.order.OrderStatus;
import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final MerchantService merchantService;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final NoticeService noticeService;

    public void auditMerchant(Long merchantId, boolean approve) {
        merchantService.audit(merchantId, approve);
    }

    public List<MerchantView> listMerchants(String status) {
        var q = Wrappers.<Merchant>lambdaQuery();
        if (status != null) {
            q.eq(Merchant::getStatus, status);
        }
        q.orderByDesc(Merchant::getId);
        return merchantMapper.selectList(q).stream()
                .map(m -> new MerchantView(m.getId(), m.getName(), m.getAddress(), m.getLng(),
                        m.getLat(), m.getRadius(), m.getStatus(), m.getRating()))
                .toList();
    }

    public Long createCategory(String name, Long parentId, Integer sort) {
        Category c = new Category();
        c.setName(name);
        c.setParentId(parentId == null ? 0L : parentId);
        c.setSort(sort == null ? 0 : sort);
        categoryMapper.insert(c);
        return c.getId();
    }

    public List<Category> listCategories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort));
    }

    public List<OrderView> listOrders(String status) {
        var q = Wrappers.<Order>lambdaQuery();
        if (status != null) {
            q.eq(Order::getStatus, status);
        }
        q.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectList(q).stream().map(orderService::toView).toList();
    }

    public void refundApprove(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (OrderStatus.COMPLETED.name().equals(order.getStatus())
                || OrderStatus.SERVICING.name().equals(order.getStatus())
                || OrderStatus.ACCEPTED.name().equals(order.getStatus())) {
            paymentService.refund(orderId, order.getAmount());
            order.setRefundStatus("FULL");
            order.setPayStatus("REFUNDED");
            order.setStatus(OrderStatus.REFUNDED.name());
            orderMapper.updateById(order);
            noticeService.send(order.getConsumerId(), "REFUND_APPROVED",
                    Map.of("orderId", orderId, "amount", order.getAmount()));
            if (order.getMerchantId() != null) {
                noticeService.send(order.getMerchantId(), "REFUND_NOTICE", Map.of("orderId", orderId));
            }
        } else {
            throw new BizException(ResultCode.BAD_REQUEST, "该订单状态不支持仲裁退款");
        }
    }
}
