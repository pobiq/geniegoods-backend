package com.example.geniegoods.dto.goods;

import com.example.geniegoods.entity.GoodsEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "굿즈 상세보기 응답")
public class GoodsDetailDTO {
    @Schema(description = "굿즈 PK", example = "1")
    private Long goodsId;

    @Schema(description = "카테고리 한글이름", example = "핸드폰케이스")
    private String categoryKoreanName;

    @Schema(description = "조회 수", example = "2")
    private int viewCount;

    @Schema(description = "굿즈 만든사람 닉네임", example = "사과")
    private String creatorNickname;

    @Schema(description = "만든 시간", example = "2026-01-14")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @Schema(description = "스타일", example = "(일러스트, 실사, 페인팅) 중 1개")
    private String goodsStyle;

    @Schema(description = "색감", example = "(따뜻, 차분, 비비드) 중 1개")
    private String goodsTone;

    @Schema(description = "분위기", example = "(미니멀, 캐주얼, 고급) 중 1개")
    private String goodsMood;

    @Schema(description = "프롬프트", example = "사진속 객체를 화풍은 일러스트 색감은 차분 분위기는 미니멀 하게 핸드폰케이스로 만들어줘")
    private String prompt;

    @Schema(description = "굿즈 url", example = "https://kr.object.ncloudstorage.com")
    private String goodsUrl;

    public static GoodsDetailDTO of(GoodsEntity goods) {
        return GoodsDetailDTO.builder()
                .goodsId(goods.getGoodsId())
                .categoryKoreanName(goods.getGoodsCategoryEntity().getKoreanName())
                .viewCount(0)
                .creatorNickname(goods.getUser().getNickname())
                .createdAt(goods.getCreatedAt())
                .goodsStyle(goods.getGoodsStyle() != null ? goods.getGoodsStyle().getKoreanName() : null)
                .goodsTone(goods.getGoodsTone() != null ? goods.getGoodsTone().getKoreanName() : null)
                .goodsMood(goods.getGoodsMood() != null ? goods.getGoodsMood().getKoreanName() : null)
                .prompt(goods.getPrompt())
                .goodsUrl(goods.getGoodsUrl())
                .build();
    }
}