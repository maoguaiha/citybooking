package com.citybooking.server.merchant;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("technician")
public class Technician extends BaseEntity {
    private Long userId;
    private Long merchantId; // 独立技师为空
    private String name;
    private String skill;
    private Double lng;
    private Double lat;
    private String status;  // PENDING / APPROVED / REJECTED
    private Double rating = 0.0;
}
