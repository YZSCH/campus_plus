package com.campus.trade.message.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "tb_message")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "from_user_id") private Long fromUserId;
    @Column(name = "to_user_id") private Long toUserId;
    @Column(columnDefinition = "TEXT") private String content;
    @Builder.Default private Integer msgType = 0;
    @Column(name = "is_read") @Builder.Default private Integer isRead = 0;
    @Column(name = "image_url", length = 500) private String imageUrl;
    @Column(name = "goods_id") private Long goodsId;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
