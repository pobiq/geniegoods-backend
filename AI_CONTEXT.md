# AI Context - GenieGoods Backend

## 🤖 AI 활용 개발 원칙

본 백엔드 프로젝트는 Cursor를 활용하여 AI와 협업하는 방식으로 개발되었습니다.

- 기본적인 CRUD 및 API 구조는 AI를 통해 생성
- 개발자는 아키텍처 설계와 인증/인가 로직에 집중
- 생성된 코드는 반드시 검증 및 보완 후 반영

👉 AI를 단순 코드 생성 도구가 아닌, 검증 가능한 협업 대상으로 활용합니다.

---

## 📌 프로젝트 개요

GenieGoods 서비스의 백엔드는 사용자 인증, 주문 처리, AI 모델 서버 연동을 담당하며, 전체 서비스의 핵심 비즈니스 로직을 처리합니다.

---

## 🧩 주요 역할

- 사용자 인증 및 권한 관리 (JWT, OAuth2)
- 주문 및 데이터 처리
- AI 모델 서버(FastAPI)와의 통신
- API 설계 및 응답 구조 관리

---

## ⚙️ 기술 스택

- Spring Boot
- Spring Security
- JWT (Access / Refresh Token)
- OAuth2
- Spring Data JPA
- MySQL
- Swagger

---

## ⚠️ AI 코드 검증 원칙

- 인증/인가 로직 (토큰 만료, 권한 체크) 검증 필수
- API 요청/응답 구조 일관성 유지
- 예외 처리 (Global Exception Handler) 적용
- 외부 서버(FastAPI) 통신 오류 대응

---

## 📝 작업 방식

- TODO.md → 구현할 기능 정의
- REVIEW.md → 코드 검증 및 보완 기록

---

## 💡 핵심 개발 방식 요약

AI에게 기본 구조와 코드 생성을 맡기고,  
저는 인증, 데이터 흐름, 안정성 검증에 집중하여 개발했습니다.