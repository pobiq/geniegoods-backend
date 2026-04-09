# Review Logs

- [2026-03-18] 이미지 저장소 로컬 전환 (Storage Refactoring) - 검토 결과 및 보안 취약점 유무: Path Traversal 취약점 발견 및 보완 완료 - 수정 사항 요약:
  - `application.yml`에 로컬 저장소 경로 및 리소스 핸들러 설정 추가.
  - `ObjectStorageService`를 `LocalStorageService`로 교체하여 로컬 파일 시스템 기반 저장 로직 구현.
  - `UserService`, `GoodsService`, `UserRestController`, `GoodsRestController`의 의존성을 `LocalStorageService`로 업데이트.
  - `WebConfig`를 추가하여 `/images/**` 경로에 대한 정적 리소스 핸들링 설정.
  - `ObjectStorageService.java` 및 `ObjectStorageConfig.java` 삭제.
  - `LocalStorageService`에 Path Traversal 방지 로직 추가.
