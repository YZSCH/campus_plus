package com.campus.trade.admin.service;

import com.campus.trade.admin.entity.Arbitration;
import com.campus.trade.admin.repository.ArbitrationRepository;
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
 * 仲裁管理服务
 */
@Slf4j
@Service
public class ArbitrationService {
    
    private final ArbitrationRepository arbitrationRepository;
    private final UserRepository userRepository;
    
    public ArbitrationService(ArbitrationRepository arbitrationRepository, UserRepository userRepository) {
        this.arbitrationRepository = arbitrationRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 用户提交仲裁请求
     */
    @Transactional
    public ApiResult<String> submitArbitration(Long applicantId, Long orderId, Long respondentId,
                                               String arbitrationType, String reason, 
                                               String evidence, String applicantClaim) {
        // 检查申请人是否存在
        Optional<User> applicantOpt = userRepository.findById(applicantId);
        if (applicantOpt.isEmpty()) {
            return ApiResult.error(404, "申请人不存在");
        }
        
        // 检查被申请人是否存在
        Optional<User> respondentOpt = userRepository.findById(respondentId);
        if (respondentOpt.isEmpty()) {
            return ApiResult.error(404, "被申请人不存在");
        }
        
        // 检查是否已经提交过该订单的仲裁
        List<Arbitration> existingArbitrations = arbitrationRepository.findByOrderId(orderId);
        for (Arbitration arb : existingArbitrations) {
            if (arb.getStatus() == 0) { // 进行中的仲裁
                return ApiResult.error(400, "该订单已有进行中的仲裁");
            }
        }
        
        // 创建仲裁记录
        Arbitration arbitration = Arbitration.builder()
                .orderId(orderId)
                .applicantId(applicantId)
                .respondentId(respondentId)
                .arbitrationType(arbitrationType)
                .reason(reason)
                .evidence(evidence)
                .applicantClaim(applicantClaim)
                .status(0) // 进行中
                .arbitrationResult("PENDING")
                .build();
        
        arbitrationRepository.save(arbitration);
        log.info("用户{}提交仲裁请求，订单ID={}, 被申请人ID={}", applicantId, orderId, respondentId);
        
        return ApiResult.success("仲裁申请已提交，等待管理员处理");
    }
    
    /**
     * 被申请人回复仲裁
     */
    @Transactional
    public ApiResult<String> replyArbitration(Long arbitrationId, Long respondentId, String reply) {
        Optional<Arbitration> arbitrationOpt = arbitrationRepository.findById(arbitrationId);
        if (arbitrationOpt.isEmpty()) {
            return ApiResult.error(404, "仲裁记录不存在");
        }
        
        Arbitration arbitration = arbitrationOpt.get();
        
        // 验证是否为被申请人
        if (!arbitration.getRespondentId().equals(respondentId)) {
            return ApiResult.error(403, "无权回复此仲裁");
        }
        
        if (arbitration.getStatus() != 0) {
            return ApiResult.error(400, "仲裁已结束，无法回复");
        }
        
        arbitration.setRespondentReply(reply);
        arbitrationRepository.save(arbitration);
        
        log.info("被申请人{}回复仲裁{}", respondentId, arbitrationId);
        
        return ApiResult.success("回复已提交");
    }
    
    /**
     * 管理员处理仲裁
     */
    @Transactional
    public ApiResult<String> handleArbitration(Long arbitrationId, Long arbitratorId,
                                               String arbitrationResult, String resultDescription) {
        Optional<Arbitration> arbitrationOpt = arbitrationRepository.findById(arbitrationId);
        if (arbitrationOpt.isEmpty()) {
            return ApiResult.error(404, "仲裁记录不存在");
        }
        
        Arbitration arbitration = arbitrationOpt.get();
        
        if (arbitration.getStatus() != 0) {
            return ApiResult.error(400, "该仲裁已处理");
        }
        
        // 更新仲裁记录
        arbitration.setArbitratorId(arbitratorId);
        arbitration.setArbitrationResult(arbitrationResult);
        arbitration.setResultDescription(resultDescription);
        arbitration.setStatus(1); // 已完成
        arbitration.setArbitratedAt(LocalDateTime.now());
        arbitrationRepository.save(arbitration);
        
        log.info("管理员{}处理仲裁{}，结果：{}", arbitratorId, arbitrationId, arbitrationResult);
        
        return ApiResult.success("仲裁处理完成");
    }
    
    /**
     * 撤销仲裁（申请人撤销）
     */
    @Transactional
    public ApiResult<String> cancelArbitration(Long arbitrationId, Long applicantId) {
        Optional<Arbitration> arbitrationOpt = arbitrationRepository.findById(arbitrationId);
        if (arbitrationOpt.isEmpty()) {
            return ApiResult.error(404, "仲裁记录不存在");
        }
        
        Arbitration arbitration = arbitrationOpt.get();
        
        // 验证是否为申请人
        if (!arbitration.getApplicantId().equals(applicantId)) {
            return ApiResult.error(403, "无权撤销此仲裁");
        }
        
        if (arbitration.getStatus() != 0) {
            return ApiResult.error(400, "仲裁已结束，无法撤销");
        }
        
        arbitration.setStatus(2); // 已撤销
        arbitrationRepository.save(arbitration);
        
        return ApiResult.success("仲裁已撤销");
    }
    
    /**
     * 查询用户发起的仲裁
     */
    public ApiResult<?> getMyArbitrations(Long userId) {
        List<Arbitration> arbitrations = arbitrationRepository.findByApplicantIdOrderByCreatedAtDesc(userId);
        return ApiResult.success(arbitrations);
    }
    
    /**
     * 查询用户被仲裁的记录
     */
    public ApiResult<?> getArbitrationsAgainstUser(Long userId) {
        List<Arbitration> arbitrations = arbitrationRepository.findByRespondentIdOrderByCreatedAtDesc(userId);
        return ApiResult.success(arbitrations);
    }
    
    /**
     * 分页查询进行中的仲裁（管理员）
     */
    public ApiResult<?> listPendingArbitrations(int page, int size) {
        Page<Arbitration> pageResult = arbitrationRepository.findByStatusOrderByCreatedAtDesc(
                0, PageRequest.of(page, size));
        return ApiResult.success(pageResult);
    }
    
    /**
     * 分页查询所有仲裁记录（管理员）
     */
    public ApiResult<?> listAllArbitrations(int page, int size) {
        Page<Arbitration> pageResult = arbitrationRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        return ApiResult.success(pageResult);
    }
    
    /**
     * 获取仲裁详情
     */
    public ApiResult<?> getArbitrationDetail(Long arbitrationId) {
        Optional<Arbitration> arbitrationOpt = arbitrationRepository.findById(arbitrationId);
        if (arbitrationOpt.isEmpty()) {
            return ApiResult.error(404, "仲裁记录不存在");
        }
        return ApiResult.success(arbitrationOpt.get());
    }
    
    /**
     * 根据订单ID查询仲裁记录
     */
    public ApiResult<?> getArbitrationsByOrder(Long orderId) {
        List<Arbitration> arbitrations = arbitrationRepository.findByOrderId(orderId);
        return ApiResult.success(arbitrations);
    }
}
