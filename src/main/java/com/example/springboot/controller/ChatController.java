package com.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.ChatMessage;
import com.example.springboot.mapper.ChatMessageMapper;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RestController
public class ChatController {
    @Resource
    private ChatMessageMapper chatMessageMapper;

    private static final ConcurrentHashMap<Integer, LinkedBlockingQueue<ChatMessage>> userQueues = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Long> userLastActive = new ConcurrentHashMap<>();

    private static LinkedBlockingQueue<ChatMessage> getQueue(Integer userId) {
        return userQueues.computeIfAbsent(userId, k -> new LinkedBlockingQueue<>());
    }

    public static Long getLastActive(Integer userId) {
        return userLastActive.get(userId);
    }

    @PostMapping("/chat/send")
    public Result sendMessage(@RequestBody Map<String, Object> body) {
        Account account = TokenUtils.getCurrentUser();
        if (account == null || account.getId() == null) {
            return Result.error("401", "未登录");
        }

        Object toUserIdObj = body.get("to_user_id");
        Object contentObj = body.get("content");
        String clientMsgId = body.get("client_msg_id") == null ? null : String.valueOf(body.get("client_msg_id"));

        if (toUserIdObj == null || contentObj == null) {
            return Result.error("400", "参数不完整");
        }

        String toUserId = String.valueOf(toUserIdObj);
        String content = String.valueOf(contentObj);

        ChatMessage msg = new ChatMessage();
        msg.setFromUid(String.valueOf(account.getId()));
        msg.setToUid(toUserId);
        msg.setContent(content);
        msg.setSendTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        msg.setStatus(0);

        chatMessageMapper.insert(msg);

        try {
            getQueue(Integer.valueOf(toUserId)).offer(msg);
        } catch (Exception ignored) {}

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", msg.getId());
        resp.put("created_at", msg.getSendTime());
        resp.put("created_at_epoch", System.currentTimeMillis());
        resp.put("client_msg_id", clientMsgId);
        return Result.success(resp);
    }

    @GetMapping("/chat/poll")
    public Result poll(@RequestParam(required = false) Long since,
                       @RequestParam(required = false, defaultValue = "30") Integer timeoutSeconds) throws InterruptedException {
        Account account = TokenUtils.getCurrentUser();
        if (account == null || account.getId() == null) {
            return Result.error("401", "未登录");
        }
        Integer uid = account.getId();
        userLastActive.put(uid, System.currentTimeMillis());

        List<ChatMessage> messages = new ArrayList<>();

        if (since != null) {
            List<ChatMessage> dbMessages = chatMessageMapper.selectList(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getToUid, String.valueOf(uid))
                            .gt(ChatMessage::getId, since)
                            .orderByAsc(ChatMessage::getId)
            );
            messages.addAll(dbMessages);
        }

        if (messages.isEmpty()) {
            ChatMessage first = getQueue(uid).poll(timeoutSeconds, TimeUnit.SECONDS);
            if (first != null) {
                messages.add(first);
                getQueue(uid).drainTo(messages, 49);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("messages", messages);
        resp.put("next_since", messages.isEmpty() ? since : messages.get(messages.size() - 1).getId());
        resp.put("server_time", System.currentTimeMillis());
        resp.put("suggested_poll_interval", suggestInterval(messages));
        return Result.success(resp);
    }

    private int suggestInterval(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 45;
        }
        int count = messages.size();
        if (count >= 20) return 5;
        if (count >= 5) return 10;
        return 30;
    }

    @PostMapping("/chat/ack/read")
    public Result ackRead(@RequestBody Map<String, Object> body) {
        Account account = TokenUtils.getCurrentUser();
        if (account == null || account.getId() == null) {
            return Result.error("401", "未登录");
        }
        Object fromObj = body.get("from_user_id");
        if (fromObj == null || StringUtils.isBlank(String.valueOf(fromObj))) {
            return Result.error("400", "缺少参数from_user_id");
        }
        int updated = chatMessageMapper.updateMessageStatusToRead(String.valueOf(account.getId()), String.valueOf(fromObj));
        Map<String, Object> resp = new HashMap<>();
        resp.put("updated", updated);
        return Result.success(resp);
    }

    @PostMapping("/chat/ack/delivered")
    public Result ackDelivered(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("updated", 0);
        return Result.success(resp);
    }

    @GetMapping("/chat/history")
    public Result getChatHistory(@RequestParam String uid1,
                                 @RequestParam String uid2) {
        List<ChatMessage> list = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getFromUid, uid1).eq(ChatMessage::getToUid, uid2)
                        .or()
                        .eq(ChatMessage::getFromUid, uid2).eq(ChatMessage::getToUid, uid1)
                        .orderByAsc(ChatMessage::getId)
        );
        Map<String, Object> resp = new HashMap<>();
        resp.put("messages", list);
        resp.put("next_cursor", list.isEmpty() ? null : list.get(list.size() - 1).getId());
        return Result.success(resp);
    }
}