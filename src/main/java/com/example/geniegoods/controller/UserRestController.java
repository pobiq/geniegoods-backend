package com.example.geniegoods.controller;

import com.example.geniegoods.dto.common.CommonResponseDTO;
import com.example.geniegoods.dto.user.*;
import com.example.geniegoods.entity.UserEntity;
import com.example.geniegoods.repository.UserRepository;
import com.example.geniegoods.security.JwtUtil;
import com.example.geniegoods.service.LocalStorageService;
import com.example.geniegoods.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "User API", description = "사용자 프로필 관리 및 인증 관련 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserRestController {

    private final UserService userService;
    private final LocalStorageService localStorageService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Operation(summary = "현재 사용자 정보 조회", description = "Cookie에 저장된 AccessToken을 통해 현재 로그인한 사용자의 정보를 가져옵니다.")
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDTO> getCurrentUser(@AuthenticationPrincipal UserEntity user) {
        CurrentUserResponseDTO response = userService.getCurrentUserInfo(user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "쿠키에 저장된 AccessToken과 RefreshToken을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<CommonResponseDTO> logout(HttpServletResponse response) {
        // AccessToken 쿠키 삭제
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // 즉시 삭제
        accessTokenCookie.setAttribute("SameSite", "Lax"); // SameSite 속성도 설정
        response.addCookie(accessTokenCookie);
        
        // RefreshToken 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // 즉시 삭제
        refreshTokenCookie.setAttribute("SameSite", "Lax"); // SameSite 속성도 설정
        response.addCookie(refreshTokenCookie);
        
        return ResponseEntity.ok(
                CommonResponseDTO
                .builder()
                .message("로그아웃 되었습니다.")
                .build()
        );
    }

    @Operation(summary = "refreshToken 발급", description = "RefreshToken으로 AccessToken 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponseDTO> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            // 쿠키에서 refreshToken 추출
            Cookie[] cookies = request.getCookies();
            String refreshToken = null;
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
            
            if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body(
                        CommonResponseDTO.builder()
                                .message("Invalid refresh token")
                                .build());
            }
            
            // refreshToken에서 userId 추출
            Long userId = jwtUtil.getUserIdFromToken(refreshToken);
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // 새로운 accessToken 생성
            String newAccessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getNickname());
            
            // 새로운 accessToken 쿠키 설정
            Cookie accessTokenCookie = new Cookie("accessToken", newAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(false);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(30 * 60); // 30분
            accessTokenCookie.setAttribute("SameSite", "Lax");
            response.addCookie(accessTokenCookie);
            
            return ResponseEntity.ok(
                    CommonResponseDTO
                            .builder()
                            .message("Token refreshed")
                            .build());
        } catch (Exception e) {
            log.error("토큰 갱신 실패", e);
            return ResponseEntity.status(401).body(
                    CommonResponseDTO
                            .builder()
                            .message("Token refresh failed")
                            .build());
        }
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임 중복 확인")
    @GetMapping("/nickname/check")
    public ResponseEntity<NickCheckResponseDTO> checkNicknameDuplicate(
            @Schema(description = "입력한 닉네임", example = "사과")
            @RequestParam("nickname") String nickname,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(userService.isNicknameExists(currentUser, nickname));
    }

    @Operation(summary = "닉네임 변경", description = "닉네임 변경")
    @PatchMapping("/nickname/update")
    public ResponseEntity<NickUpdateResponseDTO> updateNickname(
            @RequestBody NickUpdateRequestDTO request,
            @AuthenticationPrincipal UserEntity currentUser) {

        return ResponseEntity.ok(userService.updateNickname(currentUser, request.getNickname()));
    }

    @Operation(summary = "accessToken 발급", description = "포스트맨 테스트용")
    @GetMapping("/token/{userId}")
    public ResponseEntity<Map<String, String>> getToken(
            @Schema(description = "유저 PK", example = "1")
            @PathVariable("userId") Long userId
    ) {
        Map<String, String> response = userService.getToken(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 이미지 업로드", description = "프로필 이미지 업로드")
    @PostMapping("/profile-image")
    public ResponseEntity<ProfileImgResponseDTO> uploadProfileImage(
            @AuthenticationPrincipal UserEntity currentUser,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(userService.updateProfileImage(currentUser, file));
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴시 재가입 기간 30일 동안 막음")
	@DeleteMapping("/me/withdraw")
	public ResponseEntity<WithDrawResponseDTO> withdraw(
	        @AuthenticationPrincipal UserEntity currentUser) {

        return ResponseEntity.ok(userService.withdrawUser(currentUser.getUserId()));


	}

}
