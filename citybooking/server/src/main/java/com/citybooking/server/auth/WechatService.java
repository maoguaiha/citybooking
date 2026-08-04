package com.citybooking.server.auth;

import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

/**
 * 微信小程序登录：用 {@code wx.login} 拿到的 code 换取 openid。
 * 开发/测试态 {@code app.wechat.mock=true} 时直接把 code 映射成 mock openid，免去真实微信配置。
 */
@Service
public class WechatService {

    private final String appid;
    private final String secret;
    private final boolean mock;
    private final RestTemplate http = new RestTemplate();

    public WechatService(@Value("${app.wechat.appid:}") String appid,
                         @Value("${app.wechat.secret:}") String secret,
                         @Value("${app.wechat.mock:true}") boolean mock) {
        this.appid = appid;
        this.secret = secret;
        this.mock = mock;
    }

    public String exchangeOpenid(String code) {
        if (mock) {
            return "mock_" + (code == null ? "dev" : code);
        }
        if (appid.isBlank() || secret.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "微信登录配置缺失");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid
                + "&secret=" + secret + "&js_code=" + code + "&grant_type=authorization_code";
        Map<String, Object> resp = http.getForObject(URI.create(url), Map.class);
        if (resp == null || resp.get("openid") == null) {
            Object err = resp == null ? "empty" : resp.get("errmsg");
            throw new BizException(ResultCode.BAD_REQUEST, "微信登录失败: " + err);
        }
        return String.valueOf(resp.get("openid"));
    }
}
