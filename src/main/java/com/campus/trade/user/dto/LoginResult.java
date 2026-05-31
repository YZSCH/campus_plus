package com.campus.trade.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class LoginResult {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private Integer authStatus;
    private String avatarUrl;
    private Integer creditScore;
}
