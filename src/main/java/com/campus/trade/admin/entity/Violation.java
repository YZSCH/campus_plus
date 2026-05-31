package com.campus.trade.admin.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 违规行为记录表
 */
@Entity
@Table(name = "tb_violation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Violation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "violation_type", nullable = false, length = 50)
    private String violationType; // 违规类型：SPAM, FRAUD, ABUSE, OTHER

    @Column(name = "violation_level", length = 20)
    private String violationLevel; // 违规等级：LIGHT, MEDIUM, SEVERE

    @Column(name = "description", length = 500)
    private String description; // 违规描述

    @Column(name = "evidence", length = 1000)
    private String evidence; // 证据材料（JSON格式存储）

    @Column(name = "handler_id")
    private Long handlerId; // 处理人ID

    @Column(name = "handle_method", length = 50)
    private String handleMethod; // 处理方式：WARNING, DEDUCT_CREDIT, BAN_TEMP, BAN_PERMANENT

    @Column(name = "deduct_points")
    @Builder.Default
    private Integer deductPoints = 0; // 扣除信用分

    @Column(name = "ban_until")
    private LocalDateTime banUntil; // 封禁截止时间（临时封禁）

    @Column(name = "status")
    @Builder.Default
    private Integer status = 0; // 0-待处理, 1-已处理, 2-已撤销

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

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
