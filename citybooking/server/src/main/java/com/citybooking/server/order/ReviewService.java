package com.citybooking.server.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.dto.OrderDto.ReviewReq;
import com.citybooking.server.merchant.Merchant;
import com.citybooking.server.merchant.MerchantMapper;
import com.citybooking.server.merchant.Technician;
import com.citybooking.server.merchant.TechnicianMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final TechnicianMapper technicianMapper;
    private final OrderService orderService;

    public void reviewOrder(Long orderId, Long uid, ReviewReq req) {
        if (req.score() < 1 || req.score() > 5) {
            throw new BizException(ResultCode.BAD_REQUEST, "评分需在 1-5 之间");
        }
        Order order = orderService.requireOrder(orderId);
        if (!order.getConsumerId().equals(uid)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能评价自己的订单");
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "订单未完成，暂不可评价");
        }
        if (reviewMapper.selectCount(Wrappers.<Review>lambdaQuery().eq(Review::getOrderId, orderId)) > 0) {
            throw new BizException(ResultCode.CONFLICT, "该订单已评价");
        }
        Review review = new Review();
        review.setOrderId(orderId);
        review.setConsumerId(uid);
        review.setScore(req.score());
        review.setComment(req.comment());
        reviewMapper.insert(review);
        updateRatings(order);
    }

    private void updateRatings(Order order) {
        Double mr = reviewMapper.avgMerchantScore(order.getMerchantId());
        if (mr != null) {
            Merchant m = merchantMapper.selectById(order.getMerchantId());
            if (m != null) {
                m.setRating(Math.round(mr * 100.0) / 100.0);
                merchantMapper.updateById(m);
            }
        }
        if (order.getTechnicianId() != null) {
            Double tr = reviewMapper.avgTechnicianScore(order.getTechnicianId());
            if (tr != null) {
                Technician t = technicianMapper.selectById(order.getTechnicianId());
                if (t != null) {
                    t.setRating(Math.round(tr * 100.0) / 100.0);
                    technicianMapper.updateById(t);
                }
            }
        }
    }
}
