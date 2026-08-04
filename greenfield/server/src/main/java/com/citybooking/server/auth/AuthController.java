package com.citybooking.server.auth;

import com.citybooking.server.common.ApiResponse;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.common.SecurityUtil;
import com.citybooking.server.dto.AuthDto.AuthResp;
import com.citybooking.server.dto.AuthDto.LoginReq;
import com.citybooking.server.dto.AuthDto.RegisterReq;
import com.citybooking.server.dto.AuthDto.UserInfo;
import com.citybooking.server.dto.AuthDto.WechatLoginReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResp> register(@RequestBody @Valid RegisterReq req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResp> login(@RequestBody @Valid LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfo> me() {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null) {
            throw new com.citybooking.server.common.BizException(ResultCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(authService.me(uid));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<AuthResp> wechatLogin(@RequestBody @Valid WechatLoginReq req) {
        return ApiResponse.ok(authService.wechatLogin(req.code()));
    }
}
