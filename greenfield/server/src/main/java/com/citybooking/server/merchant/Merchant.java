package com.citybooking.server.merchant;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("merchant")
public class Merchant extends BaseEntity {
    private Long userId;
    private String name;
    private String logo;
    private String address;
    private Double lng;
    private Double lat;
    private Integer radius; // 服务半径（米）
    private String status;  // PENDING / APPROVED / REJECTED
    private Double rating = 0.0;
}
