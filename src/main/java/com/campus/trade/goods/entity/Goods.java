package com.campus.trade.goods.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "tb_goods")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Goods {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(nullable = false, length = 100) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 50) private String category;
    @Column(nullable = false) private BigDecimal price;
    @Column(columnDefinition = "TEXT") private String images;
    @Builder.Default private String status = "onsale";
    @Column(name = "contact_phone", length = 20) private String contactPhone;
    @Column(length = 50) private String wechat;
    @Column(length = 100) private String location;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
