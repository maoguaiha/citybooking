package com.citybooking.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MerchantDto {

    public record OnboardReq(
            @NotBlank String name,
            String address,
            @NotNull Double lng,
            @NotNull Double lat,
            Integer radius) {
    }

    public record TechnicianReq(
            @NotBlank String name,
            String skill,
            @NotNull Double lng,
            @NotNull Double lat,
            Long merchantId) {
    }

    public record ServiceReq(
            @NotNull Long categoryId,
            @NotBlank String title,
            String description,
            @NotNull @Positive BigDecimal price,
            @NotNull Integer durationMin,
            LocalDateTime availableStart,
            LocalDateTime availableEnd) {
    }

    public record MerchantView(Long id, String name, String address, Double lng, Double lat,
                               Integer radius, String status, Double rating) {
    }

    public record ServiceView(Long id, Long merchantId, Long technicianId, Long categoryId,
                              String title, String description, BigDecimal price, Integer durationMin,
                              LocalDateTime availableStart, LocalDateTime availableEnd, String status,
                              String merchantName, Double merchantRating, Double distanceM) {
    }
}
