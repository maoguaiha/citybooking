package com.citybooking.server.order;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("payment")
public class Payment extends BaseEntity {
    private Long orderId;
    private String channel; // mock / wechat / alipay
    private String tradeNo;
    private BigDecimal amount;
    private String status;  // PENDING / PAID / FAILED / REFUNDED
    private LocalDateTime paidAt;
}
