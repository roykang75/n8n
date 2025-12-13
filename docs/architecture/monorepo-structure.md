# 모노레포 구조

n8n은 pnpm workspaces를 이용한 모노레po 구조로 관리됩니다. 이를 통해 여러 패키지를 효율적으로 관리하고 의존성을 공유할 수 있습니다.

## 전체 구조

```
n8n/
├── packages/                          # 모든 패키지의 루트
│   ├── @n8n/                         # 내부 공유 패키지들
│   │   ├── api-types/                # 프론트/백엔드 공유 타입
│   │   ├── config/                   # 설정 관리
│   │   ├── di/                       # 의존성 주입 컨테이너
│   │   ├── errors/                   # 공통 에러 클래스
│   │   ├── permissions/              # 권한 관리
│   │   ├── utils/                    # 유틸리티 함수들
│   │   └── ...                       # 기타 공유 패키지
│   ├── cli/                          # 백엔드 서버 및 CLI
│   ├── core/                         # 워크플로우 엔진
│   ├── frontend/                     # 프론트엔드 관련 패키지들
│   │   ├── editor-ui/                # Vue.js 메인 애플리케이션
│   │   └── @n8n/                     # 프론트엔드 공유 패키지
│   │       ├── design-system/        # UI 컴포넌트 라이브러리
│   │       ├── i18n/                 # 국제화
│   │       ├── stores/               # Pinia 스토어
│   │       └── composables/          # Vue 컴포저블
│   ├── nodes-base/                   # 빌트인 노드들
│   ├── nodes-langchain/              # LangChain AI 노드들
│   ├── workflow/                     # 워크플로우 타입 정의
│   ├── node-dev/                     # 노드 개발 도구
│   ├── testing/                      # 테스트 유틸리티
│   └── extensions/                   # 확장 관련 패키지
├── scripts/                          # 빌드 및 배포 스크립트들
├── docs/                             # 프로젝트 문서
├── pnpm-workspace.yaml              # 워크스페이스 설정
├── package.json                      # 루트 패키지 설정
├── turbo.json                        # Turbo 빌드 설정
└── ...
```

## 주요 패키지 상세 분석

### 1. `@n8n/api-types`
Frontend와 Backend 간의 타입 정의를 공유하는 패키지입니다.
- API 요청/응답 타입
- 워크플로우 데이터 구조
- 사용자 및 권한 관련 타임

### 2. `cli`
Express.js 기반의 백엔드 서버와 CLI 도구를 포함합니다.
- REST API 엔드포인트
- 웹훅 처리
- CLI 명령어 (n8n start, n8n worker 등)
- 미들웨어 및 라우터 설정

### 3. `core`
워크플로우 실행의 핵심 로직을 담당합니다.
- 워크플로우 파서 및 실행 엔진
- 노드 실행 관리
- 데이터 흐름 제어
- 에러 처리 및 재시도 로직

### 4. `frontend/editor-ui`
Vue 3 기반의 메인 프론트엔드 애플리케이션입니다.
- 워크플로우 에디터
- 실행 결과 뷰어
- 설정 및 관리 페이지
- 사용자 인터페이스

### 5. `frontend/@n8n/design-system`
재사용 가능한 UI 컴포넌트 라이브러리입니다.
- 버튼, 폼, 모달 등 기본 컴포넌트
- 디자인 토큰 및 테마
- Storybook 문서

### 6. `nodes-base`
400개 이상의 빌트인 노드들을 포함합니다.
- 각 서비스별 통합 노드
- HTTP 요청/응답 처리
- 데이터 변환 노드
- 트리거 노드

### 7. `workflow`
워크플로우 관련 타입과 인터페이스 정의입니다.
- IWorkflow, INode 형태 정의
- 워크플로우 유효성 검사
- 데이터 구조 표준

## 의존성 관리

### 패키지 간 의존성 그래프

```mermaid
graph TD
    A[editor-ui] --> B[@n8n/api-types]
    A --> C[@n8n/design-system]
    A --> D[@n8n/i18n]
    A --> E[@n8n/stores]
    F[cli] --> B
    F --> G[core]
    F --> H[nodes-base]
    G --> I[workflow]
    G --> J[@n8n/errors]
    H --> B
    H --> I
    K[nodes-langchain] --> H
    K --> B
```

### 패키지 버전 관리

pnpm workspace를 사용하여 내부 패키지 버전을 자동으로 관리합니다:

```yaml
# pnpm-workspace.yaml
packages:
  - packages/*
  - packages/@n8n/*
  - packages/frontend/**
  - packages/extensions/**
  - packages/testing/**

catalog:
  # 공통 의존성 버전 정의
  '@types/node': '^20.17.50'
  typescript: '5.9.2'
  # ...
```

## 빌드 시스템

### Turbo를 이용한 병렬 빌드

```json
// turbo.json
{
  "pipeline": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": ["dist/**"]
    },
    "test": {
      "dependsOn": ["build"],
      "outputs": []
    },
    "dev": {
      "cache": false,
      "persistent": true
    }
  }
}
```

### 빌드 명령어

```bash
# 전체 패키지 빌드
pnpm build

# 특정 패키지만 빌드
pnpm --filter=n8n-editor-ui build

# 개발 모드 실행
pnpm dev           # 모든 패키지
pnpm dev:be        # 백엔드만
pnpm dev:fe        # 프론트엔드만
pnpm dev:ai        # AI/LLM 관련만
```

## 개발 워크플로우

### 1. 새 기능 개발 시
1. 관련 패키지 디렉토리로 이동
2. 기능 구현
3. 테스트 코드 작성
4. 타입 체크 및 린트 실행
5. 빌드 테스트

### 2. 크로스 패키지 변경 시
1. API 타입 추가/수정 (@n8n/api-types)
2. 프론트엔드 구현 (editor-ui)
3. 백엔드 구현 (cli)
4. 통합 테스트
5. 빌드 검증

### 3. 새 패키지 추가 시
1. packages 디렉토리에 추가
2. package.json 작성
3. pnpm-workspace.yaml에 패키지 경로 확인
4. turbo.json에 빌드 설정 추가
5. 다른 패키지에서 의존성 설정

## 패키지별 역할

### 내부 공유 패키지 (@n8n/*)
- **di**: 의존성 주입 컨테이너, IoC 패턴 구현
- **errors**: 공통 에러 클래스 (UnexpectedError, OperationalError 등)
- **utils**: 문자열, 날짜, 유효성 검사 등 유틸리티
- **permissions**: 역할 기반 접근 제어 (RBAC)
- **config**: 환경 변수 및 설정 관리
- **db**: 데이터베이스 공통 코드

### 프론트엔드 전용 패키지
- **design-system**: 재사용 가능한 UI 컴포넌트
- **i18n**: 다국어 번역 관리
- **stores**: Pinia 상태 관리 스토어
- **composables**: Vue 3 Composition API 유틸리티
- **chat**: 채팅 관련 기능 (EE)

### 노드 관련 패키지
- **nodes-base**: 기본 빌트인 노드
- **nodes-langchain**: AI/LLM 기반 노드 (LangChain)
- **node-dev**: 노드 개발 보조 도구

### 테스트 패키지
- **testing**: 테스트 유틸리티 및 헬퍼 함수
- **backend-test-utils**: 백엔드 테스트 공용 코드

## 장점

1. **코드 공유**: 여러 패키지에서 공통 코드 재사용
2. **의존성 관리**: 중복 의존성 제거, 버전 일관성 유지
3. **개발 경험**: 패키지 간 자동 링크, 빠른 개발 사이클
4. **빌드 성능**: Turbo로 병렬 빌드, 캐시 활용
5. **모듈성**: 각 기능을 독립적인 패키지로 분리

## 주의사항

1. **순환 의존성**: 패키지 간 순환 참조 피하기
2. **버전 관리**: 내부 패키지 버전 정책 준수
3. **빌드 순서**: 의존성 고려한 빌드 순서 설정
4. **테스트 격리**: 각 패키지 독립적인 테스트 환경