package com.citybooking.server;

import com.citybooking.server.dto.OrderDto.CreateOrderReq;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIT {

    protected static final String BASE = "";

    /** 种子超级管理员（由 DataInitializer 播种，见 application 默认配置）。 */
    protected static final String ADMIN_PHONE = "10000000000";
    protected static final String ADMIN_PWD = "Admin@123456";

    @Autowired
    private TestRestTemplate autowiredRest;
    protected RestTemplate rest;
    @Autowired
    protected ObjectMapper om;

    @org.junit.jupiter.api.BeforeEach
    void initRest() {
        String rootUri = autowiredRest.getRootUri();
        rest = new RestTemplateBuilder()
                .rootUri(rootUri)
                .requestFactory(NoAuthRequestFactory.class)
                .errorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                        return false;
                    }

                    @Override
                    public void handleError(org.springframework.http.client.ClientHttpResponse response) {
                    }
                })
                .build();
    }

    static class NoAuthRequestFactory extends JdkClientHttpRequestFactory {
    }

    private static final AtomicLong SEQ = new AtomicLong(1000);

    protected static class Fixture {
        String consumerToken;
        String merchantToken;
        String adminToken;
        Long merchantId;
        Long serviceId;
        Long categoryId;
    }

    protected String uniqPhone(String tag) {
        long n = SEQ.incrementAndGet();
        return "13" + String.format("%09d", n % 1_000_000_000L);
    }

    protected String token(String phone, String pwd, String role) {
        Map<String, Object> reg = Map.of("phone", phone, "password", pwd, "nickname", phone, "role", role);
        try {
            rest.exchange(BASE + "/auth/register", HttpMethod.POST, json(reg), String.class);
        } catch (Exception ignored) {
        }
        ResponseEntity<String> r = rest.exchange(BASE + "/auth/login", HttpMethod.POST,
                json(Map.of("phone", phone, "password", pwd)), String.class);
        return readData(r.getBody(), "token");
    }

    /** 登录种子超级管理员，返回其 JWT。 */
    protected String adminToken() {
        ResponseEntity<String> r = rest.exchange(BASE + "/auth/login", HttpMethod.POST,
                json(Map.of("phone", ADMIN_PHONE, "password", ADMIN_PWD)), String.class);
        return readData(r.getBody(), "token");
    }

    protected Fixture setupMerchant() {
        Fixture f = new Fixture();
        f.adminToken = adminToken();
        f.merchantToken = token(uniqPhone("m"), "pwd123", "MERCHANT");
        f.consumerToken = token(uniqPhone("c"), "pwd123", "CONSUMER");
        f.categoryId = post("/admin/categories?name=测试类目", null, f.adminToken, Long.class);
        f.merchantId = post("/merchant/onboard", Map.of(
                "name", "测试商家", "address", "北京", "lng", 116.40, "lat", 39.90, "radius", 5000),
                f.merchantToken, Long.class);
        post("/admin/merchants/" + f.merchantId + "/audit?approve=true", null, f.adminToken, Void.class);
        f.serviceId = post("/merchant/services", Map.of(
                "categoryId", f.categoryId, "title", "保洁3小时", "description", "专业保洁",
                "price", 99.0, "durationMin", 180), f.merchantToken, Long.class);
        return f;
    }

    protected Long createAppointOrder(Fixture f) {
        CreateOrderReq req = new CreateOrderReq(f.serviceId, "APPOINT", f.merchantId, null,
                "客户地址", 116.41, 39.91, null);
        return post("/orders", req, f.consumerToken, Long.class);
    }

    protected Long createGrabOrder(Fixture f) {
        CreateOrderReq req = new CreateOrderReq(f.serviceId, "GRAB", null, null,
                "客户地址", 116.40, 39.90, null);
        return post("/orders", req, f.consumerToken, Long.class);
    }

    protected <T> T post(String path, Object body, String token, Class<T> cls) {
        HttpEntity<?> e = json(body, token);
        ResponseEntity<String> r = rest.postForEntity(BASE + path, e, String.class);
        assertOk(r);
        return readData(r.getBody(), cls);
    }

    protected <T> T get(String path, String token, Class<T> cls) {
        ResponseEntity<String> r = rest.exchange(BASE + path, HttpMethod.GET, json(null, token), String.class);
        assertOk(r);
        return readData(r.getBody(), cls);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getMap(String path, String token) {
        return get(path, token, Map.class);
    }

    protected List<?> getList(String path, String token) {
        ResponseEntity<String> r = rest.exchange(BASE + path, HttpMethod.GET, json(null, token), String.class);
        assertOk(r);
        try {
            JsonNode data = om.readTree(r.getBody()).get("data");
            return om.convertValue(data, List.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected HttpStatusCode statusOf(String path, HttpMethod method, Object body, String token) {
        ResponseEntity<String> r = rest.exchange(BASE + path, method, json(body, token), String.class);
        return r.getStatusCode();
    }

    private HttpEntity<?> json(Object body) {
        return json(body, null);
    }

    private HttpEntity<?> json(Object body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.setBearerAuth(token);
        }
        return new HttpEntity<>(body, h);
    }

    private void assertOk(ResponseEntity<String> r) {
        if (r.getStatusCode().isError()) {
            throw new AssertionError("HTTP " + r.getStatusCode() + " body=" + r.getBody());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readData(String json, Class<T> cls) {
        try {
            JsonNode root = om.readTree(json);
            JsonNode data = root.get("data");
            if (cls == Void.class || data == null || data.isNull()) {
                return null;
            }
            return om.treeToValue(data, cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String readData(String json, String field) {
        try {
            JsonNode root = om.readTree(json);
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                throw new IllegalStateException("响应缺少 data 字段，body=" + json);
            }
            JsonNode fieldNode = data.get(field);
            if (fieldNode == null) {
                throw new IllegalStateException("data 中无字段[" + field + "]，body=" + json);
            }
            return fieldNode.asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
