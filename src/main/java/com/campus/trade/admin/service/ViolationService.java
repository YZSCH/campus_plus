package com.campus.trade.admin.service;

import com.campus.trade.admin.entity.Violation;
import com.campus.trade.admin.repository.ViolationRepository;
import com.campus.trade.common.ApiResult;
import com.campus.trade.message.service.MessageService;
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
 * 违规行为管理服务
 */
@Slf4j
@Service
public class ViolationService {
    
    private final ViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    
    public ViolationService(ViolationRepository violationRepository, UserRepository userRepository, MessageService messageService) {
        this.violationRepository = violationRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }
    
    /**
     * 录入违规行为
     */
    @Transactional
    public ApiResult<String> createViolation(Long userId, String violationType, String violationLevel, 
                                             String description, String evidence) {
        // 检查用户是否存在
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResult.error(404, "用户不存在");
        }
        
        // 创建违规记录
        Violation violation = Violation.builder()
                .userId(userId)
                .violationType(violationType)
                .violationLevel(violationLevel)
                .description(description)
                .evidence(evidence)
                .status(0) // 待处理
                .build();
        
        violationRepository.save(violation);
        log.info("录入违规行为：用户ID={}, 违规类型={}, 违规等级={}", userId, violationType, violationLevel);
        
        return ApiResult.success("违规行为已记录");
    }
    
    /**
     * 处理违规行为
     */
    @Transactional
    public ApiResult<String> handleViolation(Long violationId, Long handlerId, String handleMethod, 
                                             Integer deductPoints, LocalDateTime banUntil) {
        Optional<Violation> violationOpt = violationRepository.findById(violationId);
        if (violationOpt.isEmpty()) {
            return ApiResult.error(404, "违规记录不存在");
        }
        
        Violation violation = violationOpt.get();
        if (violation.getStatus() != 0) {
            return ApiResult.error(400, "该违规记录已处理");
        }
        
        // 更新违规记录
        violation.setHandlerId(handlerId);
        violation.setHandleMethod(handleMethod);
        violation.setDeductPoints(deductPoints != null ? deductPoints : 0);
        violation.setBanUntil(banUntil);
        violation.setStatus(1); // 已处理
        violation.setHandledAt(LocalDateTime.now());
        violationRepository.save(violation);
        
        // 根据处理方式更新用户状态
        User user = userRepository.findById(violation.getUserId()).get();
        String notifyTitle = "";
        String notifyContent = "";
        
        switch (handleMethod) {
            case "WARNING":
                // 警告，不修改状态
                log.info("对用户{}发出警告", violation.getUserId());
                notifyTitle = "违规警告";
                notifyContent = "您的账户因违规行为收到警告：" + violation.getDescription() + "。请遵守平台规则，否则将影响您的信用分。";
                break;
                
            case "DEDUCT_CREDIT":
                // 扣除信用分
                int newScore = Math.max(0, user.getCreditScore() - violation.getDeductPoints());
                user.setCreditScore(newScore);
                userRepository.save(user);
                log.info("扣除用户{}信用分{}，当前信用分：{}", violation.getUserId(), violation.getDeductPoints(), newScore);
                notifyTitle = "信用分扣除";
                notifyContent = "您的账户因违规行为被扣除" + violation.getDeductPoints() + "信用分。违规原因：" + violation.getDescription() + "。当前信用分：" + newScore + "分。";
                break;
                
            case "BAN_TEMP":
                // 临时封禁
                user.setStatus(1); // 禁用状态
                userRepository.save(user);
                log.info("临时封禁用户{}，截止时间：{}", violation.getUserId(), banUntil);
                notifyTitle = "账号临时封禁";
                notifyContent = "您的账户因违规行为被临时封禁至" + banUntil + "。违规原因：" + violation.getDescription() + "。封禁期间无法进行交易操作。";
                break;
                
            case "BAN_PERMANENT":
                // 永久封禁
                user.setStatus(1);
                userRepository.save(user);
                log.info("永久封禁用户{}", violation.getUserId());
                notifyTitle = "账号永久封禁";
                notifyContent = "您的账户因严重违规行为被永久封禁。违规原因：" + violation.getDescription() + "。如有异议，请联系客服。";
                break;
        }
        
        // 发送系统通知给用户
        messageService.sendSystemMessage(violation.getUserId(), notifyTitle, notifyContent, null);
        
        return ApiResult.success("违规处理完成");
    }
    
    /**
     * 撤销违规记录
     */
    @Transactional
    public ApiResult<String> cancelViolation(Long violationId) {
        Optional<Violation> violationOpt = violationRepository.findById(violationId);
        if (violationOpt.isEmpty()) {
            return ApiResult.error(404, "违规记录不存在");
        }
        
        Violation violation = violationOpt.get();
        violation.setStatus(2); // 已撤销
        violationRepository.save(violation);
        
        return ApiResult.success("违规记录已撤销");
    }
    
    /**
     * 查询用户的违规记录
     */
    public ApiResult<?> getUserViolations(Long userId) {
        List<Violation> violations = violationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ApiResult.success(violations);
    }
    
    /**
     * 分页查询违规记录（管理员）
     */
    public ApiResult<?> listViolations(Integer status, int page, int size) {
        Page<Violation> pageResult;
        if (status == null) {
            pageResult = violationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        } else {
            pageResult = violationRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
        }
        return ApiResult.success(pageResult);
    }
    
    /**
     * 获取违规记录详情
     */
    public ApiResult<?> getViolationDetail(Long violationId) {
        Optional<Violation> violationOpt = violationRepository.findById(violationId);
        if (violationOpt.isEmpty()) {
            return ApiResult.error(404, "违规记录不存在");
        }
        return ApiResult.success(violationOpt.get());
    }
}