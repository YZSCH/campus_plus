package com.campus.trade.wallet.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包交易记录表
 * 记录每一笔资金变动（支付、收款、退款等）
 */
@Entity
@Table(name = "tb_wallet_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    /** 交易类型 */
    public static final String TYPE_PAY      = "PAY";       // 买家付款（资金进入担保）
    public static final String TYPE_RECEIVE  = "RECEIVE";   // 卖家收款（订单完成后）
    public static final String TYPE_REFUND   = "REFUND";    // 退款
    public static final String TYPE_RECHARGE = "RECHARGE";  // 充值

    /** 交易状态 */
    public static final String STATUS_PENDING  = "PENDING";   // 处理中（担保中）
    public static final String STATUS_COMPLETED = "COMPLETED";  // 已完成
    public static final String STATUS_REFUNDED = "REFUNDED";  // 已退款

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联订单ID */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 关联支付单号 */
    @Column(name = "pay_id", length = 50)
    private String payId;

    /** 用户ID（付款方或收款方） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 交易类型：PAY / RECEIVE / REFUND */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /** 交易金额 */
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    /** 交易状态：PENDING / COMPLETED / REFUNDED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    /** 备注 */
    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
