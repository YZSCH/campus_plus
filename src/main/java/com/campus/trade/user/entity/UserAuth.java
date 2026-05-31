package com.campus.trade.user.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 校园身份认证记录 - 对应 tb_user_auth
 */
@Entity
@Table(name = "tb_user_auth")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(name = "real_name", length = 50)
    private String realName;

    /** 学生证照片URL（模拟） */
    @Column(name = "id_card_url", length = 500)
    private String idCardUrl;

    /** 校园卡照片URL（模拟） */
    @Column(name = "campus_card_url", length = 500)
    private String campusCardUrl;

    /** 审核状态: 0=待审核 1=已通过 2=已拒绝 */
    @Builder.Default
    private Integer status = 0;

    /** 审核意见（拒绝原因） */
    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    /** 审核管理员ID */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
