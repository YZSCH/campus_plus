package com.campus.trade.user.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;
    @Column(unique = true, nullable = false, length = 20)
    private String phone;
    @Column(name = "student_id", unique = true, length = 20)
    private String studentId;
    @Column(name = "real_name", length = 50)
    private String realName;
    @Column(length = 100)
    private String department;
    @Column(name = "class_name", length = 50)
    private String className;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(name = "credit_score")
    @Builder.Default private Integer creditScore = 100;
    @Column(name = "auth_status")
    @Builder.Default private Integer authStatus = 0;
    @Column(nullable = false, length = 20)
    @Builder.Default private String role = "user";
    @Builder.Default private Integer status = 0;
    @Column(name = "login_fail_count")
    @Builder.Default private Integer loginFailCount = 0;
    @Column(name = "lock_time")
    private LocalDateTime lockTime;
    /** 钱包余额（元） */
    @Column(name = "balance", nullable = false)
    @Builder.Default private java.math.BigDecimal balance = new java.math.BigDecimal("300.00");

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
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}