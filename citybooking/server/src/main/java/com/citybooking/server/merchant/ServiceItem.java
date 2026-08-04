package com.citybooking.server.merchant;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("service_item")
public class ServiceItem extends BaseEntity {
    private Long merchantId;
    private Long technicianId; // 独立技师可为空（归属商家）
    private Long categoryId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer durationMin;
    private LocalDateTime availableStart;
    private LocalDateTime availableEnd;
    private String status; // ON / OFF
}
