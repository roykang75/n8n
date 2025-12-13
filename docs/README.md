# n8n 코드베이스 문서

이 문서는 n8n 코드베이스의 전체 구조와 아키텍처를 분석하여 정리한 자료입니다.

## 문서 목차

### 1. 전체 아키텍처 (Architecture)
- [프로젝트 개요](architecture/project-overview.md)
- [모노레포 구조](architecture/monorepo-structure.md)
- [핵심 패키지 의존성](architecture/package-dependencies.md)
- [데이터 흐름 아키텍처](architecture/data-flow.md)

### 2. 프론트엔드 (Frontend)
- [Vue.js 애플리케이션 구조](frontend/vue-app-structure.md)
- [상태 관리 (Pinia)](frontend/state-management.md)
- [컴포넌트 아키텍처](frontend/component-architecture.md)
- [디자인 시스템](frontend/design-system.md)

### 3. 백엔드 (Backend)
- [Express.js 서버 구조](backend/express-server.md)
- [API 아키텍처](backend/api-architecture.md)
- [데이터베이스 계층](backend/database-layer.md)
- [인증 및 권한 관리](backend/auth.md)

### 4. 워크플로우 엔진 (Workflow Engine)
- [워크플로우 실행 구조](workflow/execution-engine.md)
- [태스크 스케줄링](workflow/task-scheduling.md)
- [에러 처리 및 재시도](workflow/error-handling.md)

### 5. 노드 시스템 (Node System)
- [노드 아키텍처](nodes/node-architecture.md)
- [커뮤니티 노드](nodes/community-nodes.md)
- [노드 개발 가이드](nodes/node-development.md)

### 6. 개발 환경 (Development)
- [설치 및 설정](development/setup.md)
- [빌드 시스템](development/build-system.md)
- [테스트 전략](development/testing.md)
- [CI/CD 파이프라인](development/cicd.md)

### 7. 한국어 문서 (Korean)
- [아키텍처 개요](korean/architecture-overview.md)
- [개발 가이드](korean/development-guide.md)
- [기여 방법](korean/contribution-guide.md)

## 시작하기

새로운 개발자가 n8n 코드베이스를 이해하려면 다음 순서로 문서를 읽는 것을 권장합니다:

1. [프로젝트 개요](architecture/project-overview.md) - n8n의 전체적인 이해
2. [모노레포 구조](architecture/monorepo-structure.md) - 프로젝트 구조 파악
3. [개발 환경 설정](development/setup.md) - 개발 환경 구축
4. 관심 있는 분야의 상세 문서 참조

## 기여 방법

문서 개선이 필요한 경우 GitHub Issues를 통해 제안해주세요.

## 라이선스

이 문서는 n8n 프로젝트의 일부로 동일한 라이선스를 따릅니다.