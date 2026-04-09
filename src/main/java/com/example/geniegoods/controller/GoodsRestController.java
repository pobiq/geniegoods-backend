package com.example.geniegoods.controller;


import com.example.geniegoods.dto.common.CommonResponseDTO;
import com.example.geniegoods.dto.goods.*;
import com.example.geniegoods.entity.UserEntity;
import com.example.geniegoods.service.GoodsService;
import com.example.geniegoods.service.LocalStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Goods API", description = "굿즈 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsRestController {

    private final GoodsService goodsService;

    private final LocalStorageService localStorageService;

    @DeleteMapping("/bulk")
    @Operation(summary = "선택된 굿즈 일괄 삭제", description = "선택된 굿즈 일괄 삭제")
    public ResponseEntity<CommonResponseDTO> bulkDeleteGoods(
            @AuthenticationPrincipal UserEntity currentUser,
            @RequestParam(name = "goodsIds") List<Long> goodsIds) {

        goodsService.deleteGoodsByIds(
            goodsIds, 
            currentUser.getUserId()
        );

        return ResponseEntity.ok(CommonResponseDTO.builder()
                .message("선택된 굿즈가 삭제되었습니다")
                .build());
    }

    @Operation(summary = "굿즈 이미지 생성", description = "굿즈 이미지 생성 및 Object Storage 에 저장")
    @PostMapping("/create-image")
    public ResponseEntity<CreateGoodsImgResponseDTO> createGoodsImage(
            @AuthenticationPrincipal UserEntity user,
            @ModelAttribute CreateGoodsImgRequestDTO dto
    ) {
        return ResponseEntity.ok(goodsService.createGoodsImage(user, dto));
    }

    @Operation(summary = "굿즈 시안 생성", description = "나노바나나 api 통해서 굿즈 시안 이미지 기존 포함 3개 생성")
    @PostMapping("/create-goods-sample")
    public ResponseEntity<CreateGoodsSampleResponseDTO> createGoodsSample(
            @AuthenticationPrincipal UserEntity user,
            @ModelAttribute CreateGoodsSampleRequestDTO dto
    ) {

        return ResponseEntity.ok(goodsService.createGoodsSample(user, dto));
    }

    @Operation(summary = "굿즈 선택에서 뒤로가기", description = "뒤로가기시 LocalStorage에 있는 sample 폴더 이미지 삭제")
    @PostMapping("/delete-goods-sample")
    public ResponseEntity<DeleteGoodsSampleResponseDTO> deleteSampleImg(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody DeleteGoodsSampleRequestDTO dto
    ) {

        return ResponseEntity.ok(goodsService.deleteSampleImg(dto));
    }

    @Operation(summary = "시안 이미지 다운로드", description = "시안 이미지 다운로드")
    @GetMapping("/download-image")
    public ResponseEntity<byte[]> downloadImage(
            @RequestParam("url") String imageUrl
    ) {
       return goodsService.downloadImage(imageUrl);
    }

    @Operation(summary = "여러 이미지 다운로드", description = "여러 이미지를 ZIP 파일로 다운로드")
    @GetMapping("/download-images-zip")
    public ResponseEntity<byte[]> downloadImagesAsZip(
            @RequestParam("urls") List<String> imageUrls
    ) {
        return goodsService.downloadImagesAsZip(imageUrls);
    }

    @Operation(summary = "시안 선택", description = "시안 선택화면에서 시안 선택시 굿즈 테이블에 insert")
    @PostMapping("/select-goods")
    public ResponseEntity<SelectGoodsResponseDTO> selectGoods(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody SelectGoodsRequestDTO dto
    ) {
        return ResponseEntity.ok(goodsService.selectGoods(user, dto));
    }

    @Operation(summary = "내가 생성한 굿즈 리스트", description = "내가 생성한 굿즈 리스트 불러오기")
    @GetMapping("/select-all-my-goods")
    public ResponseEntity<List<SelectAllMyGoodsResponseDTO>> selectAllMyGoods(
            @AuthenticationPrincipal UserEntity user
    ) {
        return ResponseEntity.ok(goodsService.selectAllMyGoods(user));
    }

    @Operation(summary = "오늘 생성한 굿즈 개수 조회", description = "오늘 생성한 굿즈 개수 조회")
    @GetMapping("/today-count")
    public ResponseEntity<GoodsTodayCountResponseDTO> getTodayGoodsCount(
            @AuthenticationPrincipal UserEntity user
    ) {
        return ResponseEntity.ok(goodsService.getTodayGoodsCount(user.getUserId()));
    }

    @Operation(summary = "굿즈 둘러보기", description = "굿즈 둘러보기 (비회원도 가능)")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/browse")
    public List<GoodsBrowseDTO> browseGoods(
            @Schema(description = "카테고리 PK", example = "1")
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @Schema(description = "페이지", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Schema(description = "사이즈", example = "8")
            @RequestParam(name = "size", defaultValue = "8") int size) {

        if (categoryId != null && categoryId == 0) {
            categoryId = null;
        }

        Pageable pageable = PageRequest.of(page, size);
        return goodsService.browseGoods(categoryId, pageable);
    }

    @Operation(summary = "굿즈 상세보기", description = "굿즈 상세보기 (비회원도 가능) 비회원인 경우 조회수 카운트 안셈")
    @PostMapping("/view-goods")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public ResponseEntity<GoodsDetailDTO> viewGoods(
            @RequestBody GoodsDetailRequestDTO dto,
            @AuthenticationPrincipal UserEntity user) {

        return ResponseEntity.ok(goodsService.viewGoods(dto.getGoodsId(), user));
    }

}
