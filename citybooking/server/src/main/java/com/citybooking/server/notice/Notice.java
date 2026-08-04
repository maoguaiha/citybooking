package com.citybooking.server.notice;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("notice")
public class Notice extends BaseEntity {
    private Long receiverId;
    private String type;
    private String payload;
    private Boolean read;
}
