package com.citybooking.server.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.common.SecurityUtil;
import com.citybooking.server.config.JwtTokenProvider;
import com.citybooking.server.dto.AuthDto.AuthResp;
import com.citybooking.server.dto.AuthDto.LoginReq;
import com.citybooking.server.dto.AuthDto.RegisterReq;
import com.citybooking.server.dto.AuthDto.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<String> ROLES = Set.of("CONSUMER", "MERCHANT", "TECHNICIAN", "ADMIN");
    private static final String PHONE_RE = "^1[3-9]\\d{9}$";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final WechatService wechatService;

    public AuthResp register(RegisterReq req) {
        if (req.phone() == null || !req.phone().matches(PHONE_RE)) {
            throw new BizException(ResultCode.BAD_REQUEST, "手机号格式不正确");
        }
        if (req.role() == null || !ROLES.contains(req.role())) {
            throw new BizException(ResultCode.BAD_REQUEST, "角色不合法");
        }
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getPhone, req.phone())) > 0) {
            throw new BizException(ResultCode.CONFLICT, "手机号已注册");
        }
        User user = new User();
        user.setPhone(req.phone());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null ? req.phone() : req.nickname());
        user.setRole(req.role());
        user.setStatus(1);
        userMapper.insert(user);
        return new AuthResp(user.getId(), jwt.createToken(user.getId(), user.getRole()), user.getRole());
    }

    public AuthResp login(LoginReq req) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, req.phone()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "手机号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return new AuthResp(user.getId(), jwt.createToken(user.getId(), user.getRole()), user.getRole());
    }

    public AuthResp wechatLogin(String code) {
        String openid = wechatService.exchangeOpenid(code);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getWxOpenid, openid));
        if (user == null) {
            user = new User();
            user.setWxOpenid(openid);
            user.setNickname("微信用户");
            user.setRole("CONSUMER");
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return new AuthResp(user.getId(), jwt.createToken(user.getId(), user.getRole()), user.getRole());
    }

    public UserInfo me(Long uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return new UserInfo(user.getId(), user.getPhone(), user.getNickname(), user.getRole(), user.getStatus());
    }

    public User requireUser(Long uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
