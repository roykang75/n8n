# n8n Spring Boot Backend

n8n의 Node.js 백엔드를 Spring Boot 3.5로 마이그레이션한 프로젝트입니다.

## 기술 스택

- **Framework**: Spring Boot 3.5
- **Language**: Java (JDK 21)
- **Build Tool**: Gradle
- **Database**: MySQL 8
- **Cache**: Redis
- **ORM**: Spring Data JPA + Hibernate
- **Migration**: Flyway

## 실행 방법

### 1. 사전 요구사항

- JDK 21 이상
- MySQL 8
- Redis
- Gradle 8.x

### 2. 데이터베이스 설정

```sql
CREATE DATABASE n8n;
CREATE USER 'n8n_user'@'localhost' IDENTIFIED BY 'n8n_password';
GRANT ALL PRIVILEGES ON n8n.* TO 'n8n_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 애플리케이션 실행

```bash
# 개발 환경
./gradlew bootRun

# 프로덕션 환경
./gradlew bootRun -Dspring.profiles.active=prod
```

## 프로젝트 구조

```
src/main/java/xyz/oiio/n8n/
├── N8nApplication.java      # 메인 애플리케이션 클래스
├── config/                  # 설정 클래스
├── controller/              # REST API 컨트롤러
├── service/                 # 비즈니스 로직
├── repository/              # JPA 리포지토리
├── entity/                  # 데이터베이스 엔티티
├── dto/                     # 데이터 전송 객체
├── workflow/                # 워크플로우 엔진
├── node/                    # 노드 시스템
├── execution/               # 실행 관리
├── security/                # 인증/인가
└── exception/               # 예외 처리
```

## 설명

이 프로젝트는 n8n의 백엔드 기능을 Spring Boot로 재구현한 것입니다. 기존의 Node.js/TypeScript 백엔드와 호환되는 API를 제공하며, 프론트엔드는 수정 없이 사용할 수 있습니다.

주요 기능:
- 워크플로우 CRUD 및 실행
- 사용자 관리 및 인증
- 크리덴셜 안전한 저장
- 실시간 실행 상태 모니터링 (WebSocket)
- 확장 가능한 노드 시스템

## 개발 가이드

### 새로운 기능 추가

1. 필요한 엔티티를 `entity` 패키지에 생성
2. JPA 리포지토리를 `repository` 패키지에 구현
3. 비즈니스 로직을 `service` 패키지에 작성
4. REST API를 `controller` 패키지에 구현
5. 필요한 경우 데이터베이스 마이그레이션 스크립트를 `db/migration`에 추가

### 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "*WorkflowServiceTest"
```

## 기여 방법

1. 이슈 생성 또는 할당
2. 기능 브랜치 생성
3. 코드 작성 및 테스트
4. PR 생성 및 코드 리뷰
5. 메인 브랜치 머지