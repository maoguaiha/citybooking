package com.citybooking.server.payment;

public record PaymentResult(String tradeNo, boolean success) {
}
