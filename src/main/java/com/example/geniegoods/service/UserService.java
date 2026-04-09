package com.example.geniegoods.service;

import com.example.geniegoods.dto.user.*;
import com.example.geniegoods.entity.SubScribeEntity;
import com.example.geniegoods.entity.UserEntity;
import com.example.geniegoods.repository.GoodsRepository;
import com.example.geniegoods.repository.SubScribeRepository;
import com.example.geniegoods.repository.UserRepository;
import com.example.geniegoods.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GoodsRepository goodsRepository;  // 굿즈 비공개 처리용
    private final SubScribeRepository subScribeRepository;
    private final LocalStorageService localStorageService;
    private final JwtUtil jwtUtil;

    private static final int REJOIN_BLOCK_DAYS = 30;  // 재가입 제한 기간

    // 토큰 발급
    public Map<String, String> getToken(Long userId) {
        UserEntity user = findById(userId);

        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getNickname());
        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        return response;
    }

    // 현재 사용자 정보 조회
    public CurrentUserResponseDTO getCurrentUserInfo(UserEntity user) {
        Optional<SubScribeEntity> subScribe = Optional.empty();
        if(user.getSubscriptionPlan().equals("PRO")) {
            // 한달 이전 까지 있던 기간 구독 정보 조회
            subScribe = subScribeRepository.findByUserAndStartDateBetween(user, LocalDateTime.now().minusMonths(1), LocalDateTime.now());
        }

        LocalDateTime subscriptionExpiryDate = null;
        if(subScribe.isPresent()) {
            subscriptionExpiryDate = subScribe.get().getStartDate().plusMonths(1);
        }

        return CurrentUserResponseDTO.builder()
                .nickname(user.getNickname())
                .profileUrl(user.getProfileUrl())
                .subscriptionPlan(user.getSubscriptionPlan())
                .subscriptionExpiryDate(subscriptionExpiryDate)
                .build();
    }

    // 프로필 이미지 업데이트
    @Transactional
    public ProfileImgResponseDTO updateProfileImage(UserEntity user, MultipartFile file) {

        try {
            ProfileImgResponseDTO response = new ProfileImgResponseDTO();
            // 파일 유효성 검사
            if (file.isEmpty()) {
                throw new IllegalStateException("파일이 비어있습니다.");
            }

            // 이미지 파일인지 확인
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalStateException("이미지 파일만 업로드 가능합니다.");
            }

            // 파일 크기 제한 (10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalStateException("파일 크기는 10MB를 초과할 수 없습니다.");
            }

            // 기존 프로필 이미지가 있으면 삭제
            if (user.getProfileUrl() != null && !user.getProfileUrl().isEmpty()) {
                localStorageService.deleteImage(user.getProfileUrl());
            }

            // 새로운 프로필 이미지 업로드
            String imageUrl = localStorageService.uploadFile(file, user.getUserId(), "profile");

            // 프로필 이미지 업데이트
            user.setProfileUrl(imageUrl);
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);

            return ProfileImgResponseDTO.builder()
                    .message("프로필 이미지가 업로드되었습니다.")
                    .profileUrl(user.getProfileUrl())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드 중 오류가 발생했습니다." + e.getMessage());
        }

    }

    // 닉네임 중복 확인
    public NickCheckResponseDTO isNicknameExists(UserEntity currentUser, String nickname) {

        NickCheckResponseDTO response = new NickCheckResponseDTO();

        // 현재 사용자의 닉네임과 같으면 사용 가능
        if (currentUser != null && currentUser.getNickname().equals(nickname)) {
            response.setAvailable(false);
            response.setMessage("현재 사용 중인 닉네임입니다.");
            return response;
        }

        // 닉네임 중복 확인
        boolean available = true;
        String message = "사용 가능한 닉네임입니다.";

        long count = userRepository.countByNickname(nickname);

        if (count > 0) {
            available = false;
            message = "이미 사용 중인 닉네임입니다.";
        }

        response.setAvailable(available);
        response.setMessage(message);

        return response;
    }

    // 닉네임 업데이트
    @Transactional
    public NickUpdateResponseDTO updateNickname(UserEntity currentUser, String newNickname) {

        NickUpdateResponseDTO response = new NickUpdateResponseDTO();

        // validation
        // 프론트쪽에서 무슨 요청을 보낼지 모르니 모든 경우의 수를 막아야 함
        // 현재 사용자의 닉네임과 같으면 변경 안해도됨
        if (currentUser != null && currentUser.getNickname().equals(newNickname)) {
            throw new IllegalStateException("현재 사용 중인 닉네임입니다.");
        }

        // 닉네임 중복 확인
        long count = userRepository.countByNickname(newNickname);

        // 중복확인 했는데 있으면 변경 안해도됨
        if(count > 0) {
            response.setMessage("이미 사용 중인 닉네임입니다.");
        } else { // 실제로 닉네임 변경
            currentUser.setNickname(newNickname);
        }

        userRepository.save(currentUser);

        return NickUpdateResponseDTO.builder()
                .message("닉네임을 " + newNickname + "으로 변경했습니다.")
                .build();
    }

    // ===== 회원 탈퇴 메서드 (핵심!) =====
    @Transactional
    public WithDrawResponseDTO withdrawUser(Long userId) {

        UserEntity user = findById(userId);

        if (user.isDeleted()) {
            throw new IllegalStateException("이미 탈퇴 처리된 회원입니다.");
        }

        // 개인정보 마스킹 (복구 가능하도록 최소한으로)
        user.setNickname("탈퇴한 사용자");

        // 탈퇴 상태 변경
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);

        // 사용자가 만든 굿즈 비공개 처리
        goodsRepository.updateIsPublicByUserId(userId, false);

        return WithDrawResponseDTO.builder()
                .message("회원탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.")
                .build();
    }

    // ===== 소셜 로그인 시 재가입 제한 체크 및 복구 로직 =====
    @Transactional
    public UserEntity registerOrLogin(String socialType, String socialId, String nickname, String profileUrl) {
    	UserEntity user = userRepository.findBySocialTypeAndSocialId(socialType, socialId).orElse(null);

        if (user != null && user.isDeleted()) {
            LocalDateTime deletedAt = user.getDeletedAt();
            if (deletedAt == null) {
                deletedAt = LocalDateTime.now(); // 안전장치
            }

            LocalDateTime blockUntil = deletedAt.plusDays(REJOIN_BLOCK_DAYS);

            if (LocalDateTime.now().isBefore(blockUntil)) {
                long daysLeft = Duration.between(LocalDateTime.now(), blockUntil).toDays() + 1;
                throw new IllegalStateException(
                    "탈퇴한 계정입니다. " + daysLeft + "일 후에 재가입이 가능합니다."
                );
            }

            // 제한 기간 지남 → 자동 복구
            user.setDeleted(false);
            user.setDeletedAt(null);
            user.setNickname(nickname);           // 새 닉네임으로 복구
            user.setProfileUrl(profileUrl);
            // 굿즈 다시 공개 처리 (선택사항)
            goodsRepository.updateIsPublicByUserId(user.getUserId(), true);
            return userRepository.save(user);
        }

        // 신규 가입
        if (user == null) {
            user = UserEntity.builder()
                    .nickname(nickname)
                    .socialType(socialType)
                    .socialId(socialId)
                    .profileUrl(profileUrl)
                    .role("USER")
                    .deleted(false)
                    .build();
            return userRepository.save(user);
        }

        // 기존 정상 사용자
        return user;
    }

    // ===== 공용 조회 메서드 =====
    public UserEntity findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    // ===== 구독 만료 처리 메서드 =====
    /**
     * 구독 기간이 만료된 사용자들을 FREE 플랜으로 변경
     * @return 변경된 사용자 수
     */
    @Transactional
    public int expireSubscriptions() {
        List<SubScribeEntity> expiredSubscriptions = subScribeRepository.findExpiredSubscriptions(LocalDateTime.now());
        
        int count = 0;
        for (SubScribeEntity subscription : expiredSubscriptions) {
            UserEntity user = subscription.getUser();
            // PRO 플랜이고 아직 삭제되지 않은 사용자만 처리
            if ("PRO".equals(user.getSubscriptionPlan()) && !user.isDeleted()) {
                user.setSubscriptionPlan("FREE");
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                count++;
            }
        }
        
        return count;
    }

}