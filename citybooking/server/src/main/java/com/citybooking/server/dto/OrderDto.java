package com.citybooking.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDto {

    public record CreateOrderReq(
            @NotNull Long serviceId,
            @NotBlank String mode,          // APPOINT / GRAB
            Long merchantId,                // APPOINT 必填
            Long technicianId,              // 可选
            String address,
            Double lng,
            Double lat,
            LocalDateTime appointmentTime) {
    }

    public record GrabReq(Long technicianId) {
    }

    public record ReviewReq(
            @NotNull Integer score,
            String comment) {
    }

    public record PayResp(Long orderId, boolean paid, BigDecimal amount, String tradeNo, String channel) {
    }

    public record OrderView(Long id, String orderNo, Long consumerId, Long merchantId, Long technicianId,
                            Long serviceId, String serviceTitle, String mode, String address, Double lng, Double lat,
                            LocalDateTime appointmentTime, BigDecimal amount, String status, String payStatus,
                            String refundStatus, LocalDateTime createdAt) {
    }
}
