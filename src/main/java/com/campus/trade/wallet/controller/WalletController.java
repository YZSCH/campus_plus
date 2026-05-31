package com.campus.trade.wallet.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.order.entity.Order;
import com.campus.trade.order.repository.OrderRepository;
import com.campus.trade.wallet.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final OrderRepository orderRepo;

    public WalletController(WalletService ws, OrderRepository or) {
        walletService = ws;
        orderRepo = or;
    }

    /** 获取钱包余额 */
    @GetMapping("/balance")
    public ApiResult<?> getBalance(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return walletService.getBalance(userId);
    }

    /** 获取交易记录 */
    @GetMapping("/transactions")
    public ApiResult<?> getTransactions(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return walletService.getTransactions(userId);
    }

    /** 模拟支付（买家付款，资金进入担保） */
    @PostMapping("/pay")
    public ApiResult<?> pay(@RequestBody Map<String, Long> body, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        Long orderId = body.get("orderId");
        if (orderId == null) return ApiResult.error(400, "订单ID不能为空");

        // 获取订单金额
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return ApiResult.error(404, "订单不存在");

        String payId = "MOCK_" + System.currentTimeMillis() + "_" + userId;
        return walletService.pay(orderId, userId, payId, order.getTotalPrice());
    }

    /** 余额充值 */
    @PostMapping("/recharge")
    public ApiResult<?> recharge(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        Object amt = body.get("amount");
        if (amt == null) return ApiResult.error(400, "金额不能为空");
        BigDecimal amount;
        try {
            amount = new BigDecimal(amt.toString());
        } catch (Exception e) {
            return ApiResult.error(400, "金额格式错误");
        }
        return walletService.recharge(userId, amount);
    }
}
