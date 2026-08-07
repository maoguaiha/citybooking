package com.citybooking.server.notice;

import com.baomidou.mybatisplus.annotation.TableField;
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
    // `read` 是 MySQL 保留字，需用反引号转义，避免 MP 生成 SQL 报错
    @TableField("`read`")
    private Boolean read;
}
