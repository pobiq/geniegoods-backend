package com.example.geniegoods.dto.goods;

import com.example.geniegoods.entity.GoodsEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "굿즈 시안 선택 응답")
public class SelectAllMyGoodsResponseDTO {
    @Schema(description = "굿즈 PK", example = "1")
    private Long goodsId;
    @Schema(description = "굿즈 url", example = "https://kr.object.ncloudstorage.com")
    private String goodsUrl;
    @Schema(description = "가격", example = "10000")
    private Integer price;

    public static SelectAllMyGoodsResponseDTO of(GoodsEntity goodsEntity) {
        return SelectAllMyGoodsResponseDTO.builder()
            .goodsId(goodsEntity.getGoodsId())
            .goodsUrl(goodsEntity.getGoodsUrl())
            .price(goodsEntity.getGoodsCategoryEntity() != null ? goodsEntity.getGoodsCategoryEntity().getPrice() : 0)
            .build();
    }
}
