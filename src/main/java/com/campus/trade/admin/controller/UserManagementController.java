package com.campus.trade.admin.controller;

import com.campus.trade.admin.service.UserManagementService;
import com.campus.trade.common.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/user")
public class UserManagementController {
    
    private final UserManagementService userManagementService;
    
    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }
    
    /**
     * 分页查询用户列表
     */
    @GetMapping
    public ApiResult<?> listUsers(@RequestParam(required = false) String role,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return userManagementService.listUsers(role, status, page, size);
    }
    
    /**
     * 根据ID查询用户详情
     */
    @GetMapping("/{id}")
    public ApiResult<?> getUserById(@PathVariable Long id) {
        return userManagementService.getUserById(id);
    }
    
    /**
     * 根据学号查询用户
     */
    @GetMapping("/student/{studentId}")
    public ApiResult<?> getUserByStudentId(@PathVariable String studentId) {
        return userManagementService.getUserByStudentId(studentId);
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    public ApiResult<String> updateUserStatus(@PathVariable Long id,
                                              @RequestBody Map<String, Object> params) {
        Integer status = Integer.parseInt(params.get("status").toString());
        String reason = (String) params.get("reason");
        return userManagementService.updateUserStatus(id, status, reason);
    }
    
    /**
     * 批量更新用户状态
     */
    @PutMapping("/batch/status")
    public ApiResult<String> batchUpdateUserStatus(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        java.util.List<Long> userIds = (java.util.List<Long>) params.get("userIds");
        Integer status = Integer.parseInt(params.get("status").toString());
        String reason = (String) params.get("reason");
        return userManagementService.batchUpdateUserStatus(userIds, status, reason);
    }
    
    /**
     * 导出用户数据
     */
    @GetMapping("/export")
    public ApiResult<?> exportUsers(@RequestParam(required = false) String role,
                                     @RequestParam(required = false) Integer status) {
        return userManagementService.exportUsers(role, status);
    }
}
