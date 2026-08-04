package com.citybooking.server.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NoticeService(NoticeMapper noticeMapper, NoticeWebSocketHandler handler) {
        this.noticeMapper = noticeMapper;
        this.handler = handler;
    }

    public void send(Long receiverId, String type, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            Notice notice = new Notice();
            notice.setReceiverId(receiverId);
            notice.setType(type);
            notice.setPayload(json);
            notice.setRead(false);
            noticeMapper.insert(notice);
            handler.push(receiverId, objectMapper.writeValueAsString(Map.of(
                    "type", type, "payload", payload, "id", notice.getId())));
        } catch (Exception e) {
            // 通知失败不影响主流程
        }
    }
}
