package com.example.geniegoods.entity;

import com.example.geniegoods.enums.GoodsMood;
import com.example.geniegoods.enums.GoodsStyle;
import com.example.geniegoods.enums.GoodsTone;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TB_GOODS")
public class GoodsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GOODS_ID")
    private Long goodsId;

    @Column(name = "GOODS_URL")
    private String goodsUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "GOODS_STYLE")
    private GoodsStyle goodsStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "GOODS_TONE")
    private GoodsTone goodsTone;

    @Enumerated(EnumType.STRING)
    @Column(name = "GOODS_MOOD")
    private GoodsMood goodsMood;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "GOODS_IMG_SIZE")
    private Long goodsImgSize;

    @Lob
    @Column(name = "PROMPT", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "is_public", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isPublic = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UPLOAD_GROUP_ID", nullable = false)
    private UploadImgGroupEntity uploadImgGroup;

    // 연관관계 (필요시)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID", nullable = false)
    private GoodsCategoryEntity goodsCategoryEntity;

}