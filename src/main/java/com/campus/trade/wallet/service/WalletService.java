package com.campus.trade.wallet.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.message.service.MessageService;
import com.campus.trade.order.entity.Order;
import com.campus.trade.order.repository.OrderRepository;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.repository.UserRepository;
import com.campus.trade.wallet.entity.WalletTransaction;
import com.campus.trade.wallet.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包服务
 * 处理支付、收款、退款等资金逻辑
 *
 * 资金流：
 *   1. 买家付款 → 资金进入担保（PENDING）
 *   2. 订单完成 → 卖家收款（COMPLETED，余额增加）
 *   3. 退款 → 资金退回买家
 */
@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletTransactionRepository walletRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final MessageService messageService;

    @Autowired
    public WalletService(WalletTransactionRepository wr, UserRepository ur, OrderRepository or,
                          MessageService ms) {
        walletRepo = wr;
        userRepo = ur;
        orderRepo = or;
        messageService = ms;
    }

    // ==================== 买家付款（资金进入担保） ====================

    /**
     * 买家付款：扣减买家余额，创建 PENDING 状态的交易记录（担保）
     * 此时卖家尚未收到钱，需等订单完成后才到账
     */
    @Transactional
    public ApiResult<?> pay(Long orderId, Long buyerId, String payId, BigDecimal amount) {
        // 1. 校验订单
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return ApiResult.error(404, "订单不存在");
        if (!order.getBuyerId().equals(buyerId)) return ApiResult.error(403, "不是你的订单");
        if (!"unpaid".equals(order.getPayStatus())) return ApiResult.error(400, "订单已支付");

        // 2. 校验买家余额
        User buyer = userRepo.findById(buyerId).orElse(null);
        if (buyer == null) return ApiResult.error(404, "用户不存在");
        if (buyer.getBalance().compareTo(amount) < 0) return ApiResult.error(400, "余额不足");

        // 3. 扣减买家余额
        buyer.setBalance(buyer.getBalance().subtract(amount));
        userRepo.save(buyer);

        // 4. 创建付款交易记录（担保中，PENDING）
        WalletTransaction tx = WalletTransaction.builder()
                .orderId(orderId)
                .payId(payId)
                .userId(buyerId)
                .type(WalletTransaction.TYPE_PAY)
                .amount(amount)
                .status(WalletTransaction.STATUS_PENDING)
                .remark("支付担保（订单完成前资金托管）")
                .build();
        walletRepo.save(tx);

        // 5. 更新订单支付状态
        order.setPayId(payId);
        order.setPayStatus("paid");
        order.setStatus("paid"); // 待发货
        orderRepo.save(order);

        log.info("[钱包] 买家 {} 付款 {} 元，订单 {}，资金进入担保", buyerId, amount, orderId);
        return ApiResult.success("支付成功，资金已托管");
    }

    // ==================== 订单完成后卖家收款 ====================

    /**
     * 订单完成（买家签收）后，卖家收到钱
     */
    @Transactional
    public ApiResult<?> releaseFunds(Long orderId) {
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return ApiResult.error(404, "订单不存在");
        if (!"completed".equals(order.getStatus())) return ApiResult.error(400, "订单未完成");

        // 查找对应的付款记录
        WalletTransaction payTx = walletRepo.findByOrderIdAndType(orderId, WalletTransaction.TYPE_PAY).orElse(null);
        if (payTx == null) return ApiResult.error(404, "付款记录不存在");

        Long sellerId = order.getSellerId();
        BigDecimal amount = payTx.getAmount();

        // 增加卖家余额
        User seller = userRepo.findById(sellerId).orElse(null);
        if (seller == null) return ApiResult.error(404, "卖家不存在");
        seller.setBalance(seller.getBalance().add(amount));
        userRepo.save(seller);

        // 创建收款记录
        WalletTransaction receiveTx = WalletTransaction.builder()
                .orderId(orderId)
                .payId(order.getPayId())
                .userId(sellerId)
                .type(WalletTransaction.TYPE_RECEIVE)
                .amount(amount)
                .status(WalletTransaction.STATUS_COMPLETED)
                .remark("订单完成收款")
                .build();
        walletRepo.save(receiveTx);

        // 更新付款记录状态
        payTx.setStatus(WalletTransaction.STATUS_COMPLETED);
        walletRepo.save(payTx);

        // 发送系统通知：卖家钱到账
        try {
            messageService.sendSystemMessage(sellerId,
                    "钱已到账",
                    "订单 " + order.getOrderNo() + " 已完成，款项 ¥" + amount.toString() + " 已到账！",
                    order.getGoodsId());
        } catch (Exception e) {
            log.warn("[钱包] 发送收款通知失败", e);
        }

        log.info("[钱包] 订单 {} 完成，卖家 {} 收款 {} 元", orderId, sellerId, amount);
        return ApiResult.success("收款成功", receiveTx);
    }

    // ==================== 退款 ====================

    /**
     * 退款：将资金从担保退回买家（适用于未完成的订单）
     */
    @Transactional
    public ApiResult<?> refund(Long orderId) {
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return ApiResult.error(404, "订单不存在");

        WalletTransaction payTx = walletRepo.findByOrderIdAndType(orderId, WalletTransaction.TYPE_PAY).orElse(null);
        if (payTx == null) return ApiResult.error(404, "付款记录不存在");

        Long buyerId = order.getBuyerId();
        BigDecimal amount = payTx.getAmount();

        // 退款给买家
        User buyer = userRepo.findById(buyerId).orElse(null);
        if (buyer == null) return ApiResult.error(404, "买家不存在");
        buyer.setBalance(buyer.getBalance().add(amount));
        userRepo.save(buyer);

        // 更新交易记录
        payTx.setStatus(WalletTransaction.STATUS_REFUNDED);
        payTx.setRemark("已退款");
        walletRepo.save(payTx);

        // 更新订单状态
        order.setPayStatus("refunded");
        order.setStatus("cancelled");
        orderRepo.save(order);

        log.info("[钱包] 订单 {} 退款 {} 元给买家 {}", orderId, amount, buyerId);
        return ApiResult.success("退款成功");
    }

    // ==================== 查询 ====================

    /** 查询用户的钱包交易记录 */
    public ApiResult<List<WalletTransaction>> getTransactions(Long userId) {
        return ApiResult.success(walletRepo.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /** 查询用户余额 */
    public ApiResult<BigDecimal> getBalance(Long userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return ApiResult.error(404, "用户不存在");
        return ApiResult.success(user.getBalance());
    }

    // ==================== 充值 ====================

    @Transactional
    public ApiResult<?> recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResult.error(400, "充值金额必须大于0");
        }
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return ApiResult.error(404, "用户不存在");

        BigDecimal currentBalance = user.getBalance();
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        user.setBalance(currentBalance.add(amount));
        userRepo.save(user);

        WalletTransaction tx = WalletTransaction.builder()
                .orderId(0L)
                .userId(userId)
                .type(WalletTransaction.TYPE_RECHARGE)
                .amount(amount)
                .status(WalletTransaction.STATUS_COMPLETED)
                .remark("余额充值")
                .build();
        walletRepo.save(tx);

        log.info("[钱包] 用户 {} 充值 {} 元", userId, amount);
        return ApiResult.success("充值成功");
    }
}
