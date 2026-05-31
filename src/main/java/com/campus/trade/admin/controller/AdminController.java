package com.campus.trade.admin.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器 - 校园认证审核
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取待审核的认证列表
     */
    @GetMapping("/auth/pending")
    public ApiResult<?> pendingAuths(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return userService.listPendingAuth(page, size);
    }

    /**
     * 审核认证
     */
    @PostMapping("/auth/review")
    public ApiResult<String> reviewAuth(@RequestParam Long authId,
                                        @RequestParam boolean approved,
                                        @RequestParam(required = false) String comment,
                                        Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");
        Long reviewerId = (Long) auth.getPrincipal();
        return userService.reviewAuth(authId, reviewerId, approved, comment);
    }
}
