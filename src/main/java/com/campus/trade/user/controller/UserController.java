package com.campus.trade.user.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.user.dto.LoginRequest;
import com.campus.trade.user.dto.LoginResult;
import com.campus.trade.user.dto.RegisterRequest;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.repository.UserRepository;
import com.campus.trade.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService us, UserRepository ur) {
        userService = us; userRepository = ur;
    }

    @PostMapping("/send-code")
    public ApiResult<Void> sendCode(@RequestParam String phone) { return userService.sendCode(phone); }

    @PostMapping("/register")
    public ApiResult<LoginResult> register(@Valid @RequestBody RegisterRequest req) { return userService.register(req); }

    @PostMapping("/login")
    public ApiResult<LoginResult> login(@Valid @RequestBody LoginRequest req) { return userService.login(req); }

    @PostMapping("/logout")
    public ApiResult<Void> logout(Authentication auth) {
        if (auth != null) return userService.logout((Long) auth.getPrincipal(), (String) auth.getCredentials());
        return ApiResult.success(null);
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");
        User user = userRepository.findById((Long) auth.getPrincipal()).orElse(null);
        if (user == null) return ApiResult.error(404, "用户不存在");
        Map<String, Object> info = new HashMap<>();
        info.put("phone", user.getPhone());
        info.put("studentId", user.getStudentId());
        info.put("realName", user.getRealName());
        info.put("department", user.getDepartment());
        info.put("className", user.getClassName());
        info.put("creditScore", user.getCreditScore());
        info.put("authStatus", user.getAuthStatus());
        info.put("username", user.getUsername());
        info.put("role", user.getRole());
        info.put("avatarUrl", user.getAvatarUrl());
        return ApiResult.success(info);
    }

    @GetMapping("/info/{id}")
    public ApiResult<Map<String, Object>> getUserInfo(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ApiResult.error(404, "用户不存在");
        Map<String, Object> info = new HashMap<>();
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("creditScore", user.getCreditScore());
        return ApiResult.success(info);
    }

    // ===== 校园认证接口 =====

    @PostMapping("/auth")
    public ApiResult<String> submitAuth(Authentication auth,
                                        @RequestBody Map<String, String> body) {
        if (auth == null) return ApiResult.error(401, "未登录");
        String studentId = body.get("studentId");
        String realName = body.get("realName");
        if (studentId == null || realName == null)
            return ApiResult.error(400, "请填写学号和姓名");
        return userService.submitAuth((Long) auth.getPrincipal(), studentId, realName);
    }

    @GetMapping("/auth/status")
    public ApiResult<?> getAuthStatus(Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");
        return userService.getMyAuth((Long) auth.getPrincipal());
    }
}
