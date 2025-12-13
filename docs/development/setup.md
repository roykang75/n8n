# 개발 환경 설치 및 설정

n8n 개발 환경을 구축하고 기여하는 방법을 안내합니다.

## 시스템 요구사항

### 필수 요구사항
- **Node.js**: 22.16 이상
- **pnpm**: 10.22.0 이상
- **Git**: 최신 버전

### 권장 사양
- **RAM**: 16GB 이상
- **CPU**: 4코어 이상
- **저장 공간**: 20GB 이상의 여유 공간

## 설치 절차

### 1. 소스 코드 클론

```bash
# n8n 리포지토리 클론
git clone https://github.com/n8n-io/n8n.git
cd n8n

# 개발 브랜치 확인
git checkout develop
```

### 2. Node.js 및 pnpm 설치

#### Node.js 설치 (방법 1: NVM 사용)
```bash
# NVM 설치
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# 터미널 재시작 후
nvm install 22
nvm use 22
nvm alias default 22
```

#### Node.js 설치 (방법 2: 공식 다운로드)
- [Node.js 공식 사이트](https://nodejs.org/)에서 LTS 버전 다운로드

#### pnpm 설치
```bash
# npm을 통해 pnpm 설치
npm install -g pnpm@latest

# 또는 corepack 사용 (Node.js 16.10+)
corepack enable
corepack prepare pnpm@latest --activate
```

### 3. 의존성 설치

```bash
# 모든 패키지 의존성 설치
pnpm install

# 또는 개발 의존성만 빠르게 설치
pnpm install --frozen-lockfile
```

### 4. 환경 변수 설정

```bash
# 개발 환경용 환경 변수 파일 복사
cp .env.example .env.dev

# .env.dev 파일 편집
# 기본값:
# N8N_BASIC_AUTH_ACTIVE=true
# N8N_BASIC_AUTH_USER=admin
# N8N_BASIC_AUTH_PASSWORD=password
# N8N_HOST=localhost
# N8N_PORT=5678
# N8N_PROTOCOL=http
# WEBHOOK_URL=http://localhost:5678/
```

## 개발 모드 실행

### 전체 개발 환경

```bash
# 프론트엔드와 백엔드 동시 실행
pnpm dev

# 프로세스 분리 실행
pnpm dev:be  # 백엔드만
pnpm dev:fe  # 프론트엔드만
```

### AI/LLR 개발 환경

```bash
# LangChain 노드 개발
pnpm dev:ai
```

### 데이터베이스 설정

#### SQLite (개발용)
```bash
# 기본 설정으로 바로 사용 가능
export DB_TYPE=sqlite
export DB_SQLITE_DATABASE=~/.n8n/database.sqlite
```

#### PostgreSQL
```bash
# PostgreSQL 설치 (macOS)
brew install postgresql

# 서버 시작
brew services start postgresql

# 데이터베이스 생성
createdb n8n_dev

# 환경 변수 설정
export DB_TYPE=postgresdb
export DB_POSTGRESDB_HOST=localhost
export DB_POSTGRESDB_PORT=5432
export DB_POSTGRESDB_DATABASE=n8n_dev
export DB_POSTGRESDB_USER=postgres
export DB_POSTGRESDB_PASSWORD=
```

## 빌드 시스템

### 전체 빌드

```bash
# 전체 프로젝트 빌드 (오래 걸림)
pnpm build > build.log 2>&1

# 빌드 로그 확인
tail -n 20 build.log
```

### 패키지별 빌드

```bash
# 특정 패키지만 빌드
pnpm --filter=n8n build
pnpm --filter=n8n-editor-ui build

# 변경된 패키지만 빌드
pnpm build:changed
```

## 테스트

### 단위 테스트

```bash
# 전체 테스트 실행
pnpm test

# 특정 패키지 테스트
cd packages/cli && pnpm test

# 특정 테스트 파일
cd packages/nodes-base && pnpm test nodes/HttpbinHTTP/HttpbinHTTP.test.ts

# 테스트覆盖率
pnpm test --coverage
```

### 통합 테스트

```bash
# 통합 테스트 실행
pnpm test:integration

# 데이터베이스별 테스트
pnpm test:postgres
pnpm test:mysql
```

### E2E 테스트

```bash
# Playwright E2E 테스트
pnpm --filter=n8n-playwright test:local

# E2E 테스트 UI로 실행
pnpm --filter=n8n-playwright dev --ui
```

## 코드 품질

### 린팅

```bash
# 전체 코드 린트
pnpm lint

# 자동 수정
pnpm lint:fix

# 스타일 린트
pnpm lint:styles
pnpm lint:styles:fix
```

### 타입 체크

```bash
# 전체 타입 체크
pnpm typecheck

# 특정 패키지
cd packages/cli && pnpm typecheck
```

### 포맷팅

```bash
# 코드 포맷팅
pnpm format

# 포맷 체크
pnpm format:check
```

## 개발 도구

### VS Code 설정

#### 추천 확장
```json
{
  "recommendations": [
    "ms-vscode.vscode-typescript-next",
    "esbenp.prettier-vscode",
    "dbaeumer.vscode-eslint",
    "ms-vscode.vscode-json",
    "bradlc.vscode-tailwindcss",
    "ms-vscode.vscode-jest"
  ]
}
```

#### 워크스페이스 설정
`.vscode/settings.json`:
```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true,
    "source.organizeImports": true
  },
  "typescript.preferences.importModuleSpecifier": "relative",
  "files.exclude": {
    "**/node_modules": true,
    "**/dist": true,
    "**/.turbo": true
  }
}
```

### Git Hooks 설정

Lefthook이 자동으로 설정됩니다:
- pre-commit: 린트와 포맷팅 실행
- pre-push: 타입 체크와 테스트 실행

```bash
# 훅 수동 실행
npx lefthook run pre-commit
```

## 디버깅

### VS Code 디버깅 설정

`.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Debug n8n Backend",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/packages/cli/bin/n8n",
      "args": ["start"],
      "env": {
        "NODE_OPTIONS": "--inspect-brk"
      },
      "console": "integratedTerminal",
      "restart": true
    },
    {
      "name": "Debug Tests",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/node_modules/.bin/jest",
      "args": ["--runInBand"],
      "console": "integratedTerminal"
    }
  ]
}
```

### 로깅

```bash
# 디버그 모드
DEBUG=n8n:* pnpm dev

# 특정 모듈 로깅
DEBUG=n8n:workflow* pnpm dev

# 로그 레벨 설정
LOG_LEVEL=debug pnpm dev
```

## 데이터베이스 관리

### 마이그레이션

```bash
# 마이그레이션 실행
pnpm --filter=n8n db:migrate

# 마이그레이션 생성
pnpm --filter=n8n db:create-migration MyNewMigration

# 마이그레이션 롤백
pnpm --filter=n8n db:rollback
```

### 데이터 시드

```bash
# 테스트 데이터 시드
pnpm --filter=n8n db:seed
```

## 성능 최적화

### 빌드 최적화

```bash
# 증분 빌드
pnpm build --incremental

# 병렬 빌드
turbo run build --parallel

# 캐시 비우고 빌드
pnpm clean
pnpm build
```

### 메모리 설정

```bash
# Node.js 메모리 증가
export NODE_OPTIONS="--max-old-space-size=8192"
```

## 문제 해결

### 일반적인 문제

#### 1. 의존성 충돌
```bash
# lockfile 재생성
rm pnpm-lock.yaml
pnpm install
```

#### 2. 포트 충돌
```bash
# 사용 중인 포트 확인
lsof -i :5678

# 포트 변경
export N8N_PORT=5679
```

#### 3. 빌드 실패
```bash
# 캐시 정리
pnpm clean-all

# 강제 재설치
rm -rf node_modules .turbo
pnpm install
```

### 개발자 도구

#### 1. 패키지 의존성 시각화
```bash
# 의존성 그래프
pnpm ls --graph
```

#### 2. 번들 분석
```bash
# 프론트엔드 번들 분석
cd packages/editor-ui
pnpm build --analyze
```

## 커뮤니티 리소스

### 채널
- [Discord](https://discord.gg/n8n): 실시간 질문 및 대화
- [GitHub Discussions](https://github.com/n8n-io/n8n/discussions): 기능 제안 및 일반 논의
- [Stack Overflow](https://stackoverflow.com/questions/tagged/n8n): 기술적 질문

### 문서
- [공식 문서](https://docs.n8n.io/)
- [API 문서](https://docs.n8n.io/api/)
- [노드 개발 가이드](https://docs.n8n.io/hosting/nodes/)

## 기여 전체 과정

1. **이슈 확인**: 기여할 이슈를 선택하거나 새로 생성
2. **포크 및 클론**: GitHub에서 포크 후 로컬에 클론
3. **브랜치 생성**: 이슈 번호로 브랜치 생성
4. **개발**: 코드 구현 및 테스트
5. **커밋**: 의미 있는 커밋 메시지로 작업 저장
6. **PR 생성**: Pull Request 생성 및 설명 작성
7. **코드 리뷰**: 리뷰어 피드백 반영
8. **머지**: 승인 후 메인 브랜치에 머지

## 힌트와 팁

### 1. 생산성 향상
- 핫 리로드 활용: 변경 사항 즉시 반영
- 단축키 사용: VS Code 단축키 익히기
- 스 니펫 활용: 자주 사용하는 코드 스니펫

### 2. 디버깅 팁
- 브레이크포인트 설정: 핵심 로직에 브레이크포인트
- 로그 적극 활용: 상태 변화 로깅
- 테스트 작성: 신뢰할 수 있는 코드 작성

### 3. 코드 품질
- 타입 스트릭트: 엄격한 타입 체크
- 커밋 메시지: 명확한 의사 전달
- 리팩토링: 주기적인 코드 개선

이 가이드를 통해 n8n 개발 환경을 성공적으로 구축하고 프로젝트에 기여할 수 있습니다.