package com.campus.trade.evaluation.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "tb_evaluation")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id") private Long orderId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "target_user_id") private Long targetUserId;
    @Column(name = "goods_id") private Long goodsId;
    @Column(columnDefinition = "TEXT") private String content;
    private Integer rating;
    @Column(columnDefinition = "TEXT") private String images;
    @Builder.Default private Integer status = 0;
    @Column(name = "report_reason", length = 500) private String reportReason;
    @Column(name = "reported_at") private LocalDateTime reportedAt;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
