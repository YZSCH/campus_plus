package com.campus.trade.admin.controller;

import com.campus.trade.admin.service.ViolationService;
import com.campus.trade.common.ApiResult;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 违规行为管理控制器
 */
@RestController
@RequestMapping("/api/admin/violation")
public class ViolationController {
    
    private final ViolationService violationService;
    
    public ViolationController(ViolationService violationService) {
        this.violationService = violationService;
    }
    
    /**
     * 录入违规行为
     */
    @PostMapping
    public ApiResult<String> createViolation(@RequestBody Map<String, Object> params, Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");
        
        Long userId = Long.parseLong(params.get("userId").toString());
        String violationType = (String) params.get("violationType");
        String violationLevel = (String) params.get("violationLevel");
        String description = (String) params.get("description");
        String evidence = (String) params.get("evidence");
        
        return violationService.createViolation(userId, violationType, violationLevel, description, evidence);
    }
    
    /**
     * 处理违规行为
     */
    @PostMapping("/{id}/handle")
    public ApiResult<String> handleViolation(@PathVariable Long id, 
                                             @RequestBody Map<String, Object> params, 
                                             Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");
        
        Long handlerId = (Long) auth.getPrincipal();
        String handleMethod = (String) params.get("handleMethod");
        Integer deductPoints = params.get("deductPoints") != null ? 
                Integer.parseInt(params.get("deductPoints").toString()) : 0;
        
        LocalDateTime banUntil = null;
        if (params.get("banUntil") != null) {
            String banUntilStr = (String) params.get("banUntil");
            banUntil = LocalDateTime.parse(banUntilStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        return violationService.handleViolation(id, handlerId, handleMethod, deductPoints, banUntil);
    }
    
    /**
     * 撤销违规记录
     */
    @PostMapping("/{id}/cancel")
    public ApiResult<String> cancelViolation(@PathVariable Long id) {
        return violationService.cancelViolation(id);
    }
    
    /**
     * 查询用户的违规记录
     */
    @GetMapping("/user/{userId}")
    public ApiResult<?> getUserViolations(@PathVariable Long userId) {
        return violationService.getUserViolations(userId);
    }
    
    /**
     * 分页查询违规记录列表（管理员）
     */
    @GetMapping
    public ApiResult<?> listViolations(@RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return violationService.listViolations(status, page, size);
    }
    
    /**
     * 获取违规记录详情
     */
    @GetMapping("/{id}")
    public ApiResult<?> getViolationDetail(@PathVariable Long id) {
        return violationService.getViolationDetail(id);
    }
}
