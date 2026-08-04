package com.citybooking.server.admin;

import java.util.Set;

/**
 * 管理端角色常量。
 * SUPER_ADMIN 平台超管：可管理管理员账号 + 全部运营能力。
 * ADMIN        运营管理员：仅平台运营能力，不能管理管理员账号。
 * 两者都通过 /admin/** 的 hasAnyRole 校验放行。
 */
public final class AdminRoles {
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final Set<String> ALL = Set.of(ADMIN, SUPER_ADMIN);

    /** 普通用户可自助注册的角色（管理员角色禁止自助注册）。 */
    public static final Set<String> SELF_REGISTERABLE = Set.of("CONSUMER", "MERCHANT", "TECHNICIAN");

    private AdminRoles() {
    }
}
