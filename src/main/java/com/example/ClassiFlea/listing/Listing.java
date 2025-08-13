package com.example.ClassiFlea.listing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "LISTING")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String campus;           // 校区
    private String category;         // 分类
    @Column(name = "CONDITION_LABEL")
    private String conditionLabel;   // 成色
    private String location;         // 地点
    private BigDecimal price;        // 价格

    // 状态：active / reserved / sold / closed
    private String status;

    private String title;            // 标题

    @Column(length = 2000)
    private String description;      // 描述(可选)

    // 新增：卖家 & 图片
    private String sellerEmail;      // 卖家标识（先用邮箱）
    private String coverImage;       // 封面图片
    @Column(length = 4000)
    private String imagesJson;       // 额外图片(JSON数组)

    private Instant createdAt;       // 创建时间

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // --- getter/setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getConditionLabel() { return conditionLabel; }
    public void setConditionLabel(String conditionLabel) { this.conditionLabel = conditionLabel; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
