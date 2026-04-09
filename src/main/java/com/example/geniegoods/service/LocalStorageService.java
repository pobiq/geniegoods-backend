package com.example.geniegoods.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬 파일 시스템을 이용한 이미지 저장 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService {

    @Value("${app.storage.path}")
    private String storagePath;

    /**
     * 이미지를 로컬 저장소에 업로드
     * @param file 업로드할 파일
     * @param userId 사용자 ID
     * @param folderName 폴더명 (profile, goods 등)
     * @return 업로드된 파일의 접근 URL (또는 경로)
     */
    public String uploadFile(MultipartFile file, Long userId, String folderName) throws IOException {
        // 파일 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 고유한 파일명 생성
        String fileName = UUID.randomUUID() + extension;
        
        // 저장 경로 설정: {storagePath}/{folderName}/{userId}/{fileName}
        // 여기서는 기존 ObjectStorage 구조를 따라 folderName/uuid.ext 형식으로 저장하되 경로만 로컬로 변경
        Path directoryPath = Paths.get(storagePath, folderName);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        Path filePath = directoryPath.resolve(fileName);
        // transferTo()는 임시 파일을 이동시키거나 삭제할 수 있어, 이후에 MultipartFile을 다시 읽을 때 NoSuchFileException이 발생할 수 있음
        // 따라서 Files.copy()와 getInputStream()을 사용하여 파일을 복사함
        Files.copy(file.getInputStream(), filePath);

        log.info("파일 로컬 저장 성공: {}", filePath.toString());

        // 접근 URL 반환 (프론트엔드에서 /images/folderName/fileName 으로 접근할 수 있도록 설정 필요)
        return "/images/" + folderName + "/" + fileName;
    }

    /**
     * 로컬 저장소에서 이미지 삭제
     * @param imageUrl 삭제할 이미지의 URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            // URL에서 파일 경로 추출 (예: /images/profile/uuid.jpg)
            if (imageUrl.startsWith("/images/")) {
                String relativePath = imageUrl.substring("/images/".length());
                
                // 보안 취약점 방지 (Path Traversal)
                if (relativePath.contains("..")) {
                    log.error("유효하지 않은 경로 접근 시도: {}", relativePath);
                    return;
                }

                Path filePath = Paths.get(storagePath, relativePath);
                
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("파일 로컬 삭제 성공: {}", filePath.toString());
                }
            }
        } catch (Exception e) {
            log.error("파일 로컬 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * 로컬 저장소에서 이미지 다운로드 (바이트 배열 반환)
     * @param imageUrl 다운로드할 이미지의 URL
     * @return 이미지 바이트 배열
     */
    public byte[] downloadImage(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new IllegalArgumentException("이미지 URL이 비어있습니다.");
        }

        if (imageUrl.startsWith("/images/")) {
            String relativePath = imageUrl.substring("/images/".length());

            // 보안 취약점 방지 (Path Traversal)
            if (relativePath.contains("..")) {
                log.error("유효하지 않은 경로 접근 시도: {}", relativePath);
                throw new IOException("유효하지 않은 경로 접근입니다.");
            }

            Path filePath = Paths.get(storagePath, relativePath);
            
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                throw new IOException("파일을 찾을 수 없습니다: " + filePath.toString());
            }
        } else if (imageUrl.startsWith("http")) {
             // 외부 URL인 경우 (예: 기존 DB에 남아있는 S3 URL) - 일단 에러 처리 또는 다운로드 로직 추가 가능
             // 여기선 로컬 전환이 목적이므로 로컬 경로가 아닌 경우 에러
             throw new IOException("로컬 파일이 아닙니다: " + imageUrl);
        }
        
        throw new IOException("유효하지 않은 이미지 경로입니다: " + imageUrl);
    }
}
