package com.citybooking.server.dto;

public class AuthDto {

    public record RegisterReq(String phone, String password, String nickname, String role) {
    }

    public record LoginReq(String phone, String password) {
    }

    public record WechatLoginReq(String code) {
    }

    public record AuthResp(Long userId, String token, String role) {
    }

    public record UserInfo(Long id, String phone, String nickname, String role, Integer status) {
    }
}
