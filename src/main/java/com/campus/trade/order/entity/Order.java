package com.campus.trade.order.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "tb_order")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_no", unique = true, length = 32) private String orderNo;
    @Column(name = "goods_id") private Long goodsId;
    @Column(name = "buyer_id") private Long buyerId;
    @Column(name = "seller_id") private Long sellerId;
    @Column(name = "total_price") private BigDecimal totalPrice;
    @Builder.Default private String status = "pending";
    @Column(name = "shipping_method", length = 50) private String shippingMethod;
    @Column(columnDefinition = "TEXT") private String remark;

    /** 支付单号（关联支付宝交易） */
    @Column(name = "pay_id", length = 50)
    private String payId;

    /** 支付状态：unpaid（未支付）/ paid（已支付） */
    @Column(name = "pay_status", length = 20)
    @Builder.Default
    private String payStatus = "unpaid";

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}