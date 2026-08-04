package com.citybooking.server.order;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("review")
public class Review extends BaseEntity {
    private Long orderId;
    private Long consumerId;
    private Integer score; // 1-5
    private String comment;
}
