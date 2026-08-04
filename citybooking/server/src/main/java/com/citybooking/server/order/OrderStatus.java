package com.citybooking.server.order;

public enum OrderStatus {
    UNPAID,        // 待支付
    WAIT_ACCEPT,   // 已支付，等待商家接单（指定模式）
    PENDING_GRAB,  // 已支付，等待抢单（抢单模式）
    ACCEPTED,      // 已接单/已被抢
    SERVICING,     // 服务中
    COMPLETED,     // 已完成
    CANCELLED,     // 已取消（未支付）
    REFUNDED,      // 已退款/流单
    CLOSED         // 关闭（抢单超时流单）
}
