package com.citybooking.server.order;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("biz_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Long consumerId;
    private Long merchantId;  // 接单前可为空
    private Long technicianId; // 可空
    private Long serviceId;
    private String mode;       // APPOINT / GRAB
    private String address;
    private Double lng;
    private Double lat;
    private LocalDateTime appointmentTime;
    private BigDecimal amount;
    private String status;     // UNPAID/WAIT_ACCEPT/PENDING_GRAB/ACCEPTED/SERVICING/COMPLETED/CANCELLED/REFUNDED/CLOSED
    private String payStatus;  // UNPAID / PAID / REFUNDED
    private String refundStatus; // NONE / PARTIAL / FULL
    private LocalDateTime grabDeadline;
}
