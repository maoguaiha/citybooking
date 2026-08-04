package com.citybooking.server.merchant;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("category")
public class Category extends BaseEntity {
    private String name;
    private Long parentId;
    private Integer sort = 0;
}
