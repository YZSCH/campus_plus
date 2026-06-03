package com.campus.trade.admin.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 仲裁记录表
 */
@Entity
@Table(name = "tb_arbitration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Arbitration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId; // 关联订单ID

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId; // 申请人ID

    @Column(name = "respondent_id", nullable = false)
    private Long respondentId; // 被申请人ID

    @Column(name = "arbitration_type", nullable = false, length = 50)
    private String arbitrationType; // 仲裁类型：DISPUTE, REFUND, QUALITY, OTHER

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason; // 仲裁理由

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence; // 证据材料（JSON格式，存储Base64图片数据）

    @Column(name = "applicant_claim", length = 500)
    private String applicantClaim; // 申请人诉求

    @Column(name = "respondent_reply", length = 500)
    private String respondentReply; // 被申请人回复

    @Column(name = "arbitrator_id")
    private Long arbitratorId; // 仲裁员ID（管理员）

    @Column(name = "arbitration_result", length = 20)
    private String arbitrationResult; // 仲裁结果：PENDING, SUCCESS, FAIL, PARTIAL

    @Column(name = "result_description", length = 1000)
    private String resultDescription; // 仲裁结果说明

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 0; // 0-进行中, 1-已完成, 2-已撤销

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "arbitrated_at")
    private LocalDateTime arbitratedAt;

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