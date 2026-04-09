package com.example.geniegoods.service;

import com.example.geniegoods.dto.goods.*;
import com.example.geniegoods.entity.*;
import com.example.geniegoods.enums.GoodsMood;
import com.example.geniegoods.enums.GoodsStyle;
import com.example.geniegoods.enums.GoodsTone;
import com.example.geniegoods.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j  // log 사용을 위해 추가 (필요시)
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final UploadImgRepository uploadImgRepository;
    private final UploadImgGroupRepository uploadImgGroupRepository;
    private final GoodsCategoryRepository goodsCategoryRepository;
    private final GoodsViewRepository goodsViewRepository;
    private final YoloService yoloService;
    private final NanoService nanoService;
    private final LocalStorageService localStorageService;

    /**
     * 내가 생성한 굿즈 -> 삭제
     * @param goodsIds
     * @param currentUserId
     */
    @Transactional
    public void deleteGoodsByIds(List<Long> goodsIds, Long currentUserId) {

        if (goodsIds == null || goodsIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 굿즈 ID가 필요합니다.");
        }

        for (Long goodsId : goodsIds) {
            GoodsEntity goods = goodsRepository.findById(goodsId)
                    .orElseThrow(() -> new IllegalArgumentException("굿즈를 찾을 수 없습니다: " + goodsId));
            // 보안 체크
            if (!goods.getUser().getUserId().equals(currentUserId)) {
                throw new IllegalStateException("본인의 굿즈만 삭제할 수 있습니다.");
            }
            // isPublic 만 false로 둠
            goods.setIsPublic(false);

            goodsRepository.save(goods);
        }

    }

    /**
     * 오늘 생성한 굿즈 개수 조회
     * @param userId 사용자 ID
     * @return 오늘 생성한 굿즈 개수
     */
    public GoodsTodayCountResponseDTO getTodayGoodsCount(Long userId) {
        LocalDateTime today = LocalDateTime.now();
        int count = goodsRepository.countByUserAndCreatedAtToday(userId, today);
        return GoodsTodayCountResponseDTO.builder()
                .todayCount(count)
                .build();
    }

    /**
     * 굿즈 선택
     * @param user
     * @param dto
     * @return
     */
    @Transactional
    public SelectGoodsResponseDTO selectGoods(UserEntity user, SelectGoodsRequestDTO dto) {
        // FREE 플랜 사용자의 경우 일일 5개 제한 체크
        if ("FREE".equals(user.getSubscriptionPlan())) {

            LocalDateTime today = LocalDateTime.now();
            int count = goodsRepository.countByUserAndCreatedAtToday(user.getUserId(), today);

            if (count >= 5) {
                throw new IllegalStateException("FREE 플랜은 하루에 5개까지만 생성할 수 있습니다. PRO 플랜으로 업그레이드하시면 무제한으로 생성할 수 있습니다.");
            }
        }

        byte[] goodsImgFileByte;

        try {
            goodsImgFileByte = localStorageService.downloadImage(dto.getGoodsImgUrl());
        } catch (IOException e) {
            log.error("이미지 다운로드 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 다운로드 실패: " + e.getMessage());
        }

        MultipartFile goodsImgFile = byteArrayToMultipartFile(goodsImgFileByte);

        String goodsImgUrl = "";

        // 시안 선택한 이미지 url LocalStorage에 저장
        try {
            goodsImgUrl = localStorageService.uploadFile(goodsImgFile, user.getUserId(),
                    "goods" + "/" + user.getUserId() + "/" + dto.getUploadImgGroupId()
            );
        } catch (IOException e) {
            log.error("선택한 이미지 LocalStorage 저장 실패: {}", e.getMessage());
            throw new RuntimeException("선택한 이미지 LocalStorage 저장 실패: " + e.getMessage());
        }

        // 결과 이미지 Object Storage 삭제
        localStorageService.deleteImage(dto.getResultImageUrl());

        // 시안 3개 이미지 Object Storage 삭제
        List<String> sampleGoodsImageUrls = dto.getSampleGoodsImageUrl();

        for (String url : sampleGoodsImageUrls) {
            localStorageService.deleteImage(url);
        }

        SelectGoodsResponseDTO response = new SelectGoodsResponseDTO();

        // 카테고리 조회
        GoodsCategoryEntity category = goodsCategoryRepository.findByKoreanName(dto.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + dto.getCategory()));

        // 업로드 그룹 조회
        UploadImgGroupEntity imgGroup = uploadImgGroupRepository.findById(dto.getUploadImgGroupId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 업로드 그룹입니다: " + dto.getUploadImgGroupId()));

        // Enum 변환 (null 허용, 빈 문자열도 null로 처리)
        GoodsStyle style = (dto.getGoodsStyle() != null && !dto.getGoodsStyle().isEmpty()) 
                ? GoodsStyle.fromKoreanName(dto.getGoodsStyle()) : null;
        GoodsTone tone = (dto.getGoodsTone() != null && !dto.getGoodsTone().isEmpty()) 
                ? GoodsTone.fromKoreanName(dto.getGoodsTone()) : null;
        GoodsMood mood = (dto.getGoodsMood() != null && !dto.getGoodsMood().isEmpty()) 
                ? GoodsMood.fromKoreanName(dto.getGoodsMood()) : null;

        // GoodsEntity 생성 및 저장
        GoodsEntity goods = GoodsEntity.builder()
                .goodsUrl(goodsImgUrl)
                .goodsStyle(style)
                .goodsTone(tone)
                .goodsMood(mood)
                .goodsImgSize(goodsImgFile.getSize())
                .prompt(dto.getPrompt())
                .user(user)
                .uploadImgGroup(imgGroup)
                .goodsCategoryEntity(category)
                .build();

        goodsRepository.save(goods);

        response.setMessage("굿즈 선택 완료");
        return response;
    }

    /**
     * 뒤로가기시 Object Storage smaple 이미지 삭제
     * @param request
     * @return
     */
    public DeleteGoodsSampleResponseDTO deleteSampleImg(DeleteGoodsSampleRequestDTO request) {

        // url이 안넘어 올때
        if(request.getGoodsSampleImgUrl().isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다");
        }

        // 이미지 삭제
        for (String url : request.getGoodsSampleImgUrl()) {
            localStorageService.deleteImage(url);
        }

        return DeleteGoodsSampleResponseDTO.builder()
                .message("이미지 삭제 완료")
                .build();
    }

    /**
     * 내가 생성한 굿즈 리스트 불러오기
     * @param user
     * @return
     */
    @Transactional(readOnly = true)
    public List<SelectAllMyGoodsResponseDTO> selectAllMyGoods(UserEntity user) {
        List<GoodsEntity> goodsEntityList = goodsRepository.findByUserAndIsPublicOrderByCreatedAtDesc(user, true);

        List<SelectAllMyGoodsResponseDTO> response = new ArrayList<>();

        for(GoodsEntity goodsEntity : goodsEntityList) {
            response.add(SelectAllMyGoodsResponseDTO.of(goodsEntity));
        }

        return response;
    }
    
    public List<GoodsBrowseDTO> browseGoods(Long categoryId, Pageable pageable) {
        Page<GoodsBrowseProjection> page = goodsRepository.findByCategoryWithViews(categoryId, pageable);
        return page.getContent().stream().map(proj -> GoodsBrowseDTO.builder()
                .goodsId(proj.getGoods().getGoodsId())
                .goodsUrl(proj.getGoods().getGoodsUrl())
                .viewCount(proj.getViewCount())
                .build()).collect(Collectors.toList());
    }
    
    /**
     * 굿즈 상세 조회 (조회수 1증가)
     * @param goodsId
     * @param user
     */
    public GoodsDetailDTO viewGoods(Long goodsId, UserEntity user) {
        GoodsEntity goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new IllegalArgumentException("Goods not found"));

        if (!goods.getIsPublic()) {
            throw new IllegalArgumentException("Private goods");
        }

        // 회원이 같은 굿즈를 볼때 한번만 조회
        if (user != null) {
            GoodsViewEntity view = goodsViewRepository.findByGoodsGoodsIdAndUserUserId(goodsId, user.getUserId());
            if (view == null) {
                GoodsViewEntity newView = GoodsViewEntity.builder()
                    .goods(goods)
                    .user(user)
                    .viewedAt(LocalDateTime.now())
                    .build();
                goodsViewRepository.save(newView);
            }
        }

        GoodsDetailDTO response = GoodsDetailDTO.of(goods);

        return response;
    }

    /**
     * 굿즈 이미지 생성 (전체 플로우)
     * 1. 업로드 이미지 처리
     * 2. YOLO API 호출 -> 객체 이미지 생성
     * 3. Nano API 호출 -> 굿즈 이미지 생성
     * 4. 굿즈 이미지 Object Storage 저장
     */
    @Transactional
    public CreateGoodsImgResponseDTO createGoodsImage(UserEntity user, CreateGoodsImgRequestDTO dto) {

        List<String> uploadImgUrlList = new ArrayList<>();

        UploadImgGroupEntity uploadImgGroup = null;

        // 기존에 이미 업로드 이미지가 없으면 PK 생성
        if(dto.getPrevUploadImgGroupId() == null) {
            // 업로드 이미지 그룹 PK 생성
            uploadImgGroup = UploadImgGroupEntity.builder().build();
            uploadImgGroupRepository.save(uploadImgGroup);
        } else {
            // 업로드 이미지 그룹 찾기
            uploadImgGroup = uploadImgGroupRepository.findById(dto.getPrevUploadImgGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 업로드 그룹입니다: " + dto.getPrevUploadImgGroupId()));
        }

        // 기존에 이미 업로드 이미지가 있으면 Object Storage 에 있는 uploadImage 삭제
        if (dto.getPrevUploadImgGroupId() != null) {
            List<UploadImgEntity> uploadImg = uploadImgRepository.findByUploadImgGroup(uploadImgGroup);
            for (UploadImgEntity uploadImgEntity : uploadImg) {
                localStorageService.deleteImage(uploadImgEntity.getUploadImgUrl());
            }
        }

        // 업로드 이미지 Object Storage 저장
        if (dto.getUploadImages() != null) {
            for (MultipartFile file : dto.getUploadImages()) {
                try {
                    uploadImgUrlList.add(localStorageService.uploadFile(file, user.getUserId(),
                            "upload" + "/" + user.getUserId() + "/" + uploadImgGroup.getUploadGroupId()));
                } catch (IOException e) {
                    log.error("업로드 이미지 Object Storage 저장 실패", e);
                    throw new RuntimeException("업로드 이미지 Object Storage 저장 실패: " + e.getMessage());
                }
            }
        }

        // 기존에 업로드 이미지 그룹 아이디가 있을 경우 DB 값 삭제
        if(dto.getPrevUploadImgGroupId() != null) {
            List<UploadImgEntity> uploadImgEntityList = uploadImgRepository.findByUploadImgGroup(uploadImgGroup);
            for(UploadImgEntity uploadImgEntity : uploadImgEntityList) {
                uploadImgRepository.delete(uploadImgEntity);
            }
        }

        // 업로드 이미지 DB 저장
        if (dto.getUploadImages() != null) {
            for(int i = 0; i < dto.getUploadImages().length; i++) {
                UploadImgEntity uploadImgEntity = UploadImgEntity.builder()
                        .uploadImgUrl(uploadImgUrlList.get(i))
                        .uploadImgSize(dto.getUploadImages()[i].getSize())
                        .user(user)
                        .uploadImgGroup(uploadImgGroup)
                        .build();

                uploadImgRepository.save(uploadImgEntity);
            }
        }

        // yolo api 호출 -> 객체 이미지 생성
        List<byte[]> objectImgBytesList = yoloService.createObjectDetetctionImage(dto.getUploadImages());

        // 나노바나나 api 호출 -> 굿즈 이미지 생성
        MultipartFile goodsImgFile = nanoService.createGoodsImage(objectImgBytesList, dto);

        // 굿즈 이미지 이미 Object Storage에 저장되어있으면 삭제
        if(dto.getPrevGoodsImageUrl() != null) {
            localStorageService.deleteImage(dto.getPrevGoodsImageUrl());
        }

        // 굿즈 이미지 파일 Object Storage 저장
        String goodsImgUrl;
        try {
            goodsImgUrl = localStorageService.uploadFile(goodsImgFile, user.getUserId(), 
                    "temp" + "/" + user.getUserId() + "/" + uploadImgGroup.getUploadGroupId());
        } catch (IOException e) {
            log.error("굿즈 이미지 Object Storage 저장 실패", e);
            throw new RuntimeException("굿즈 이미지 Object Storage 저장 실패: " + e.getMessage());
        }

        return CreateGoodsImgResponseDTO.builder()
                .message("굿즈 이미지 생성 완료")
                .goodsImgUrl(goodsImgUrl)
                .goodsImgSize(goodsImgFile.getSize())
                .uploadImgGroupId(uploadImgGroup.getUploadGroupId())
                .build();
    }

    /**
     * 굿즈 시안 생성
     * 나노바나나 API를 통해 굿즈 시안 이미지 기존 포함 3개 생성
     */
    public CreateGoodsSampleResponseDTO createGoodsSample(UserEntity user, CreateGoodsSampleRequestDTO dto) {

        // 이미지 다운로드
        byte[] resultGoodsImageFileByte;
        try {
            resultGoodsImageFileByte = localStorageService.downloadImage(dto.getResultImageUrl());
        } catch (IOException e) {
            log.error("이미지 다운로드 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 다운로드 실패: " + e.getMessage());
        }

        if (resultGoodsImageFileByte == null || resultGoodsImageFileByte.length == 0) {
            throw new RuntimeException("이미지 파일 또는 URL이 없습니다.");
        }

        // byte[] -> MultipartFile 로 변환
        MultipartFile resultGoodsImageFile = byteArrayToMultipartFile(resultGoodsImageFileByte);

        // 나노바나나를 통해 2개의 시안 이미지 생성
        List<MultipartFile> sampleGoodsImages = nanoService.createGoodsSampleImage(resultGoodsImageFileByte, dto.getCategory());

        List<String> sampleImgUrls = new ArrayList<>();
        String folderPath = "sample" + "/" + user.getUserId() + "/" + dto.getUploadImgGroupId();

        // 기존 이미지 업로드
        try {
            String sampleImgUrl = localStorageService.uploadFile(resultGoodsImageFile, user.getUserId(), folderPath);
            sampleImgUrls.add(sampleImgUrl);
        } catch (IOException e) {
            log.error("시안 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("시안 생성 실패: " + e.getMessage());
        }

        // 생성된 시안 이미지들 업로드
        for (MultipartFile multipartFile : sampleGoodsImages) {
            try {
                String sampleImgUrl = localStorageService.uploadFile(multipartFile, user.getUserId(), folderPath);
                sampleImgUrls.add(sampleImgUrl);
            } catch (IOException e) {
                log.error("시안 생성 실패: {}", e.getMessage(), e);
                throw new RuntimeException("시안 생성 실패: " + e.getMessage());
            }
        }

        return CreateGoodsSampleResponseDTO.builder()
                .message("시안 생성 완료")
                .goodsSampleImgUrls(sampleImgUrls)
                .build();
    }

    /**
     * 이미지 다운로드
     * @param imageUrl
     * @return
     */
    public ResponseEntity<byte[]> downloadImage(String imageUrl) {
        try {
            byte[] imageBytes = localStorageService.downloadImage(imageUrl);
            
            // Content-Type 설정 (이미지 타입 추출)
            String contentType = "image/jpeg"; // 기본값
            if (imageUrl.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (imageUrl.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }
            
            // Content-Disposition 헤더 설정 (다운로드 파일명)
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            if (fileName.isEmpty() || !fileName.contains(".")) {
                fileName = "image.jpg";
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(imageBytes);
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 다운로드 실패: " + e.getMessage());
        }
    }

    /**
     * 이미지 여러건 다운로드
     * @param imageUrls
     */
    public ResponseEntity<byte[]> downloadImagesAsZip(List<String> imageUrls) {

        if (imageUrls == null || imageUrls.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            for (int i = 0; i < imageUrls.size(); i++) {
                String imageUrl = imageUrls.get(i);
                try {
                    byte[] imageBytes = localStorageService.downloadImage(imageUrl);

                    // 파일명 추출
                    String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    if (fileName.isEmpty() || !fileName.contains(".")) {
                        // 확장자 추출 시도
                        String contentType = "image/jpeg";
                        if (imageUrl.toLowerCase().contains(".png")) {
                            contentType = "image/png";
                            fileName = "image_" + (i + 1) + ".png";
                        } else if (imageUrl.toLowerCase().contains(".gif")) {
                            contentType = "image/gif";
                            fileName = "image_" + (i + 1) + ".gif";
                        } else {
                            fileName = "image_" + (i + 1) + ".jpg";
                        }
                    }

                    // ZIP 엔트리 추가
                    ZipEntry entry = new ZipEntry(fileName);
                    entry.setSize(imageBytes.length);
                    zos.putNextEntry(entry);
                    zos.write(imageBytes);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("이미지 다운로드 실패 (URL: {}): {}", imageUrl, e.getMessage());
                    // 실패한 이미지는 건너뛰고 계속 진행
                }
            }

            zos.close();
            byte[] zipBytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .header("Content-Type", "application/zip")
                    .header("Content-Disposition", "attachment; filename=\"goods_images.zip\"")
                    .body(zipBytes);
        } catch (Exception e) {
            log.error("ZIP 파일 생성 실패: {}", e.getMessage());
            throw new RuntimeException("ZIP 파일 생성 실패: " + e.getMessage());
        }

    }

    /**
     * byte[] -> MultipartFile 변환 헬퍼 메서드
     */
    private MultipartFile byteArrayToMultipartFile(byte[] byteArray) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "result-image";
            }

            @Override
            public String getOriginalFilename() {
                return "result-image.jpg";
            }

            @Override
            public String getContentType() {
                return "image/jpeg";
            }

            @Override
            public boolean isEmpty() {
                return byteArray == null || byteArray.length == 0;
            }

            @Override
            public long getSize() {
                return byteArray != null ? byteArray.length : 0;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return byteArray;
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new java.io.ByteArrayInputStream(byteArray);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
    }


}