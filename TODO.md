# Backend Task List

## 1. 이미지 저장소 로컬 전환 (Storage Refactoring)

- [x] **경로 설정:** `application.yml`에 `C:/Users/hanjunghun/Desktop/workspace/geniegoods/images` 설정 및 연동
- [x] **구현체 변경:** 기존 ObjectStorage 관련 로직을 `FileSystem` 기반 `LocalStorageService`로 교체

## 2. 테스트 코드 작성 (Test Suite)

- [ ] **Service Test:** 이미지 저장 및 경로 반환 로직 단위 테스트 (JUnit 5)
- [ ] **Controller Test:** MockMVC를 이용한 파일 업로드 API 엔드포인트 테스트
- [ ] **Edge Case:** 파일이 없거나 허용되지 않은 형식일 때의 예외 처리 검증

## 3. 트래픽 최적화 (Redis)

- [ ] **Redis 설정:** 로컬 환경에 Redis 연동 및 `Lettuce` 설정
- [ ] **캐싱 적용:** 잦은 조회가 발생하는 데이터에 `@Cacheable` 적용
- [ ] **성능 확인:** DB 부하 감소 여부 모니터링
