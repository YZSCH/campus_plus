package com.campus.trade.user.controller;

import com.campus.trade.admin.service.ArbitrationService;
import com.campus.trade.common.ApiResult;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户仲裁控制器
 */
@RestController
@RequestMapping("/api/arbitration")
public class UserArbitrationController {

    private final ArbitrationService arbitrationService;

    public UserArbitrationController(ArbitrationService arbitrationService) {
        this.arbitrationService = arbitrationService;
    }

    /**
     * 用户提交仲裁请求
     */
    @PostMapping
    public ApiResult<String> submitArbitration(@RequestBody Map<String, Object> params, Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");

        try {
            Long applicantId = (Long) auth.getPrincipal();

            Object orderIdObj = params.get("orderId");
            Object respondentIdObj = params.get("respondentId");

            if (orderIdObj == null || respondentIdObj == null) {
                return ApiResult.error(400, "缺少必要参数：orderId或respondentId");
            }

            Long orderId = Long.parseLong(orderIdObj.toString());
            Long respondentId = Long.parseLong(respondentIdObj.toString());
            String arbitrationType = (String) params.get("arbitrationType");
            String reason = (String) params.get("reason");
            String evidence = (String) params.get("evidence");
            String applicantClaim = (String) params.get("applicantClaim");

            if (arbitrationType == null || reason == null || applicantClaim == null) {
                return ApiResult.error(400, "缺少必要参数：arbitrationType、reason或applicantClaim");
            }

            return arbitrationService.submitArbitration(applicantId, orderId, respondentId,
                    arbitrationType, reason, evidence, applicantClaim);
        } catch (NumberFormatException e) {
            return ApiResult.error(400, "参数格式错误：orderId和respondentId必须是数字");
        } catch (Exception e) {
            return ApiResult.error(500, "提交仲裁失败：" + e.getMessage());
        }
    }

    /**
     * 被申请人回复仲裁
     */
    @PostMapping("/{id}/reply")
    public ApiResult<String> replyArbitration(@PathVariable Long id,
                                              @RequestBody Map<String, String> params,
                                              Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");

        Long respondentId = (Long) auth.getPrincipal();
        String reply = params.get("reply");

        return arbitrationService.replyArbitration(id, respondentId, reply);
    }

    /**
     * 撤销仲裁（申请人撤销）
     */
    @PostMapping("/{id}/cancel")
    public ApiResult<String> cancelArbitration(@PathVariable Long id, Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");

        Long applicantId = (Long) auth.getPrincipal();
        return arbitrationService.cancelArbitration(id, applicantId);
    }

    /**
     * 查询我发起的仲裁
     */
    @GetMapping("/my/applicant")
    public ApiResult<?> getMyArbitrations(Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");

        Long userId = (Long) auth.getPrincipal();
        return arbitrationService.getMyArbitrations(userId);
    }

    /**
     * 查询我被仲裁的记录
     */
    @GetMapping("/my/respondent")
    public ApiResult<?> getArbitrationsAgainstMe(Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");

        Long userId = (Long) auth.getPrincipal();
        return arbitrationService.getArbitrationsAgainstUser(userId);
    }

    /**
     * 获取仲裁详情
     */
    @GetMapping("/{id}")
    public ApiResult<?> getArbitrationDetail(@PathVariable Long id) {
        return arbitrationService.getArbitrationDetail(id);
    }

    /**
     * 根据订单ID查询仲裁记录
     */
    @GetMapping("/order/{orderId}")
    public ApiResult<?> getArbitrationsByOrder(@PathVariable Long orderId) {
        return arbitrationService.getArbitrationsByOrder(orderId);
    }
}
