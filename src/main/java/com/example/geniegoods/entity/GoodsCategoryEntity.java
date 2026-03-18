package com.example.geniegoods.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TB_GOODS_CATEGORY")
public class GoodsCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CATEGORY_ID")
    private Long categoryId;

    @Column(name = "KOREAN_NAME")
    private String koreanName;

    @Column(name = "PRICE")
    private Integer price;  // int로 맞춤

}