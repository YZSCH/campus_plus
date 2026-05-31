package com.campus.trade.admin.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户管理服务
 */
@Slf4j
@Service
public class UserManagementService {
    
    private final UserRepository userRepository;
    
    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 分页查询用户列表（管理员）
     */
    public ApiResult<?> listUsers(String role, Integer status, int page, int size) {
        Page<User> pageResult;
        
        if (role != null && status != null) {
            pageResult = userRepository.findByRoleAndStatus(role, status, PageRequest.of(page, size));
        } else if (role != null) {
            pageResult = userRepository.findByRole(role, PageRequest.of(page, size));
        } else if (status != null) {
            pageResult = userRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            pageResult = userRepository.findAll(PageRequest.of(page, size));
        }
        
        return ApiResult.success(pageResult);
    }
    
    /**
     * 根据ID查询用户详情
     */
    public ApiResult<?> getUserById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResult.error(404, "用户不存在");
        }
        return ApiResult.success(userOpt.get());
    }
    
    /**
     * 更新用户状态
     */
    @Transactional
    public ApiResult<String> updateUserStatus(Long userId, Integer status, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResult.error(404, "用户不存在");
        }
        
        User user = userOpt.get();
        Integer oldStatus = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);
        
        log.info("管理员更新用户{}状态：{} -> {}，原因：{}", userId, oldStatus, status, reason);
        
        String statusText = status == 0 ? "正常" : "禁用";
        return ApiResult.success("用户状态已更新为：" + statusText);
    }
    
    /**
     * 批量更新用户状态
     */
    @Transactional
    public ApiResult<String> batchUpdateUserStatus(List<Long> userIds, Integer status, String reason) {
        List<User> users = userRepository.findAllById(userIds);
        if (users.isEmpty()) {
            return ApiResult.error(404, "未找到指定用户");
        }
        
        for (User user : users) {
            user.setStatus(status);
        }
        userRepository.saveAll(users);
        
        log.info("批量更新{}个用户状态为：{}，原因：{}", userIds.size(), status, reason);
        
        String statusText = status == 0 ? "正常" : "禁用";
        return ApiResult.success("已更新" + users.size() + "个用户状态为：" + statusText);
    }
    
    /**
     * 导出用户数据（模拟）
     */
    public ApiResult<?> exportUsers(String role, Integer status) {
        List<User> users;
        
        if (role != null && status != null) {
            users = userRepository.findByRoleAndStatus(role, status);
        } else if (role != null) {
            users = userRepository.findByRole(role);
        } else if (status != null) {
            users = userRepository.findByStatus(status);
        } else {
            users = userRepository.findAll();
        }
        
        // 这里简化处理，实际应该导出为Excel或CSV文件
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", users.size());
        result.put("exportTime", LocalDateTime.now());
        result.put("users", users);
        
        log.info("导出用户数据，共{}条", users.size());
        
        return ApiResult.success(result);
    }
    
    /**
     * 根据学号查询用户
     */
    public ApiResult<?> getUserByStudentId(String studentId) {
        Optional<User> userOpt = userRepository.findByStudentId(studentId);
        if (userOpt.isEmpty()) {
            return ApiResult.error(404, "未找到该学号对应的用户");
        }
        return ApiResult.success(userOpt.get());
    }
}
