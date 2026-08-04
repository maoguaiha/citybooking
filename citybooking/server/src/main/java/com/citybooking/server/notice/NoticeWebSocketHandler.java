package com.citybooking.server.notice;

import com.citybooking.server.config.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NoticeWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider provider;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public NoticeWebSocketHandler(JwtTokenProvider provider) {
        this.provider = provider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long uid = parseUid(session);
        if (uid == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put("uid", uid);
        sessions.computeIfAbsent(uid, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long uid = (Long) session.getAttributes().get("uid");
        if (uid != null) {
            Set<WebSocketSession> set = sessions.get(uid);
            if (set != null) {
                set.remove(session);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 心跳/回执可由前端发送，这里无需处理业务
    }

    public void push(Long uid, String message) {
        Set<WebSocketSession> set = sessions.get(uid);
        if (set == null) {
            return;
        }
        for (WebSocketSession s : set) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(message));
                } catch (Exception ignored) {
                }
            }
        }
    }

    public int onlineCount(Long uid) {
        Set<WebSocketSession> set = sessions.get(uid);
        return set == null ? 0 : set.size();
    }

    private Long parseUid(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null || uri.getQuery() == null) {
                return null;
            }
            for (String kv : uri.getQuery().split("&")) {
                String[] p = kv.split("=", 2);
                if ("token".equals(p[0]) && p.length == 2) {
                    Claims c = provider.parse(java.net.URLDecoder.decode(p[1], "UTF-8"));
                    return Long.valueOf(c.getSubject());
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
