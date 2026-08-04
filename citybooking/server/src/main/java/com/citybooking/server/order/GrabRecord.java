package com.citybooking.server.order;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("grab_record")
public class GrabRecord extends BaseEntity {
    private Long orderId;
    private Long merchantId;   // 商家抢单
    private Long technicianId; // 独立技师抢单
    private String status;     // GRABBED / MISSED / TIMEOUT
}
