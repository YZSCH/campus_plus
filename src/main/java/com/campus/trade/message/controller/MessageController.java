package com.campus.trade.message.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.message.entity.Message;
import com.campus.trade.message.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/message")
public class MessageController {
    private final MessageService messageService;
    public MessageController(MessageService ms) { messageService = ms; }

    @PostMapping("/send")
    public ApiResult<Message> send(@RequestBody Message msg, Authentication auth) {
        msg.setFromUserId((Long) auth.getPrincipal());
        return messageService.send(msg);
    }

    @GetMapping("/conversation")
    public ApiResult<List<Message>> conversation(@RequestParam Long targetUserId, Authentication auth) {
        return messageService.conversation((Long) auth.getPrincipal(), targetUserId);
    }

    @GetMapping("/unread")
    public ApiResult<Long> unread(Authentication auth) {
        return messageService.unreadCount((Long) auth.getPrincipal());
    }
}
