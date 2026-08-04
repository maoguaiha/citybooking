package com.citybooking.server.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.citybooking.server.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("app_user")
public class User extends BaseEntity {
    private String phone;
    private String password;
    private String nickname;
    private String role; // CONSUMER / MERCHANT / TECHNICIAN / ADMIN
    private String wxOpenid; // 微信小程序 openid（微信授权登录）
    private Integer status = 1;
}
