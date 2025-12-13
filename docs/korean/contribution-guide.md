# n8n 기여 가이드

n8n 프로젝트에 기여하는 방법을 안내합니다. 모든 종류의 기여를 환영합니다!

## 기여 유형

### 1. 코드 기여
- 버그 수정
- 새로운 기능 개발
- 성능 개선
- 노드 개발

### 2. 문서 기여
- 번역
- 튜토리얼 작성
- API 문서 개선
- 오류 수정

### 3. 커뮤니티 기여
- 질문 답변
- 사용 팁 공유
- 피드백 제공
- 버그 리포트

## 시작하기

### 1. 사전 준비
- [GitHub 계정](https://github.com/)
- [개발 환경 설정](../development/setup.md)

### 2. 이슈 선택
- [GitHub Issues](https://github.com/n8n-io/n8n/issues)에서 기여할 이슈 탐색
- 라벨 확인:
  - `good first issue`: 초보자에게 적합
  - `help wanted`: 기여 필요
  - `bug 버그`: 버그 수정
  - `기능 개선`: 기능 추가

### 3. 브랜치 생성
```bash
# 자신의 포크 클론
git clone https://github.com/username/n8n.git
cd n8n

# 업스트림 리모트 추가
git remote add upstream https://github.com/n8n-io/n8n.git

# 최신 코드 동기화
git checkout develop
git pull upstream develop

# 브랜치 생성 (이슈 번호 포함)
git checkout -b feature/ISSUE-123-my-feature
```

## 코드 기여

### 1. 개발 가이드라인

#### 코딩 스타일
```typescript
// 좋은 예시
export class MyClass {
  private readonly property: string;

  constructor(property: string) {
    this.property = property;
  }

  async getData(): Promise<SomeType> {
    try {
      return await this.fetchData();
    } catch (error) {
      throw new NodeOperationError(this.getNode(), 'Failed to fetch data', {
        description: error.message,
      });
    }
  }
}
```

#### TypeScript 규칙
- `any` 타입 금지
- 타입 단언(`as`) 최소화
- 명확한 반환 타입 지정
```typescript
// 안 좋음
async function fetchData() {
  return await request.get('/data');
}

// 좋음
async function fetchData(): Promise<DataResponse> {
  return await request.get<DataResponse>('/data');
}
```

### 2. 새 기능 개발

#### 1단계: 설계
- 기능 명세 정의
- API 설계
- UI/UX 고려사항

#### 2단계: 구현
```bash
# 관련 패키지로 이동
cd packages/cli  # 백엔드
cd packages/editor-ui  # 프론트엔드

# 개발 시작
pnpm dev
```

#### 3단계: 테스트 작성
```typescript
// 예시: API 엔드포인트 테스트
describe('POST /api/v1/workflows', () => {
  it('should create a new workflow', async () => {
    const newWorkflow = {
      name: 'Test Workflow',
      nodes: [],
      connections: {},
    };

    const response = await request(app)
      .post('/api/v1/workflows')
      .set('Authorization', `Bearer ${token}`)
      .send(newWorkflow)
      .expect(201);

    expect(response.body.name).toBe(newWorkflow.name);
  });
});
```

### 3. 버그 수정

#### 버그 리포트 이해하기
- 재현 단계 확인
- 기대 동작 파악
- 관련 코드 위치 찾기

#### 디버깅 팁
```bash
# 디버그 로그 활성화
DEBUG=n8n:* pnpm dev

# 테스트 실행
pnpm test -- --grep "관련 테스트"

# 커버리지 확인
pnpm test --coverage --testPathPattern="파일명"
```

## 노드 개발

### 1. 새 노드 생성

#### 스켈레톤 생성
```bash
# n8n 노드 개발 도구 사용
npx n8n-node-dev create MyService

# 또는 수동으로 디렉토리 생성
mkdir packages/nodes-base/nodes/MyService
cd packages/nodes-base/nodes/MyService
```

#### 기본 파일 구조
```
MyService/
├── MyService.node.ts      # 메인 노드 클래스
├── MyService.node.json    # 노드 메타데이터
├── GenericFunctions.ts    # 공통 함수
├── test/                  # 테스트
│   └── MyService.test.ts
└── icons/                 # 아이콘
    └── myservice.svg
```

#### 노드 클래스 구현
```typescript
// MyService.node.ts
import type {
  IExecuteFunctions,
  INodeExecutionData,
  INodeType,
  INodeTypeDescription,
} from 'n8n-workflow';

export class MyService implements INodeType {
  description: INodeTypeDescription = {
    displayName: 'My Service',
    name: 'myService',
    icon: 'file:myservice.svg',
    group: ['transform'],
    version: 1,
    description: 'Interacts with My Service API',
    defaults: {
      name: 'My Service',
    },
    inputs: ['main'],
    outputs: ['main'],
    properties: [
      {
        displayName: 'Operation',
        name: 'operation',
        type: 'options',
        options: [
          {
            name: 'Get Data',
            value: 'getData',
          },
          {
            name: 'Post Data',
            value: 'postData',
          },
        ],
        default: 'getData',
      },
    ],
  };

  async execute(
    this: IExecuteFunctions
  ): Promise<INodeExecutionData[][]> {
    const items = this.getInputData();
    const operation = this.getNodeParameter('operation', 0);

    const returnData: INodeExecutionData[][] = [];

    for (let i = 0; i < items.length; i++) {
      if (operation === 'getData') {
        const id = this.getNodeParameter('id', i);

        // API 호출
        const response = await this.helpers.httpRequestWithAuthentication.call(
          this,
          'myServiceApi',
          {
            method: 'GET',
            url: `/data/${id}`,
            json: true,
          }
        );

        returnData.push([{
          json: response,
        }]);
      }
      // ... 다른 오퍼레이션
    }

    return returnData;
  }
}```

### 2. 크리더셜 추가
```typescript
// credentials/MyServiceApi.credentials.ts
import type { ICredentialType, INodeProperties } from 'n8n-workflow';

export class MyServiceApi implements ICredentialType {
  name = 'myServiceApi';
  displayName = 'My Service API';
  documentationUrl = 'https://myservice.com/docs/api';

  properties: INodeProperties[] = [
    {
      displayName: 'API Key',
      name: 'apiKey',
      type: 'string',
      typeOptions: {
        password: true,
      },
      default: '',
    },
  ];

  authenticate = {
    type: 'httpHeaderAuth',
    properties: {
      name: 'X-API-Key',
      value: '={{$credentials.apiKey}}',
    },
  };
}
```

### 3. 테스트 작성
```typescript
// test/MyService.test.ts
import { mock } from 'jest-mock-extended';
import nock from 'nock';

describe('MyService Node', () => {
  const mockNode = mock<INode>();
  const mockExecuteFunctions = mock<IExecuteFunctions>();

  beforeEach(() => {
    // 모든 모의 함수 초기화
    jest.clearAllMocks();
  });

  describe('operation: getData', () => {
    it('should fetch data successfully', async () => {
      // 설정
      mockExecuteFunctions.getNodeParameter
        .mockReturnValueOnce('getData')
        .mockReturnValueOnce('123');
      mockExecuteFunctions.getInputData.mockReturnValue([{ json: {} }]);

      // API 모의
      const scope = nock('https://api.myservice.com')
        .get('/data/123')
        .reply(200, { id: '123', name: 'Test Data' });

      // 실행
      const node = new MyService();
      const result = await node.execute.call(mockExecuteFunctions);

      // 검증
      expect(result).toHaveLength(1);
      expect(result[0][0].json).toEqual({ id: '123', name: 'Test Data' });
      scope.done();
    });
  });
});
```

## 번역

### 1. 번역 파일 구조
```
packages/@n8n/i18n/locales/
├── en/
│   ├── base.json       # 기본 번역
│   ├── nodes.json      # 노드 번역
│   └── credentials.json # 크리덴셜 번역
├── ko/
│   ├── base.json
│   ├── nodes.json
│   └── credentials.json
└── ...
```

### 2. 새 번역 추가
```json
// ko/base.json
{
  "workflow": {
    "name": "워크플로우",
    "description": "워크플로우 설명"
  },
  "nodeTypes": {
    "httpRequest": {
      "displayName": "HTTP 요청",
      "description": "HTTP 요청을 보내고 응답을 받습니다"
    }
  }
}
```

### 3. 번역 가이드라인
- 자연스러운 용어 사용
- 기술 용어는 원문 유지 또는 표준 용어 사용
- 문맥에 맞는 번역
- 일관된 용어 사용

## Pull Request(PR) 생성

### 1. PR 전 체크리스트
- [ ] 테스트 통과
- [ ] 타입 체크 통과
- [ ] 린터 통과
- [ ] 코드 스타일 따름
- [ ] 문서 업데이트
- [ ] 커밋 메시지 적절

### 2. 코드 품질 검사
```bash
# 전체 검사
pnpm lint
pnpm typecheck
pnpm test

# 특정 패키지
cd packages/cli && pnpm lint
cd packages/cli && pnpm typecheck
```

### 3. 커밋 컨벤션
```
<타입>(<범위>): <설명>

예시:
feat(core): add retry mechanism to workflow execution
fix(editor): resolve canvas scrolling issue
docs(ko): add korean translation for workflow UI
test(api): add unit tests for authentication endpoint
```

### 4. PR 템플릿
```markdown
## 설명
이 PR의 목적과 변경 내용 설명

## 변경 내용
- 무엇을 수정/추가했는지
- 어떻게 동작하는지

## 테스트
- 테스트 전략
- 수동 테스트 결과

## 관련 이슈
Closes #ISSUE_NUMBER

## 체크리스트
- [ ] 코드 리뷰
- [ ] 테스트 통과
- [ ] 문서 업데이트
```

## 코드 리뷰

### 1. 리뷰 기준
- 코드 품질
- 논리적 정확성
- 성능 영향
- 보안 고려사항

### 2. 피드백 제공
- 구체적인 제안
- 이유 설명
- 대안 제시

### 3. 피드백 수용
- 겸허하게 수용
- 질문으로 확장
- 토론에 참여

## 릴리스 프로세스

### 1. 버전 관리
- **주 버전**: 호환되지 않는 변경
- **부 버전**: 호환되는 기능 추가
- **수정 버전**: 버그 수정

### 2. 체인지로그 관리
- 중요 변경 기록
- 기여자 인정
- 마이그레이션 가이드

### 3. 배포
- CI/CD 자동화
- 버전 태깅
- 릴리스 노트 작성

## 커뮤니티 참여

### 1. 커뮤니케이션 채널
- **Discord**: [https://discord.gg/n8n](https://discord.gg/n8n)
- **GitHub Discussions**: 기술 논의
- **Twitter**: 업데이트 공유

### 2. 커뮤니티 기여 방법
- 질문에 답변하기
- 튜토리얼 공유하기
- 버그 리포트 작성
- 기능 요청

### 3. 행동 강령
- 존중과 배려
- 포괄성
- 건설적 피드백

## 기여자 인정

### 1. 기여자 등급
- **Core**: 주요 기여자
- **Active**: 정기적 기여자
- **Contributor**: 일회성 기여자

### 2. 인정 방법
- README에 기여자 목록
- 릴리스 노트에 언급
- 특별 배지 부여
- 커뮤니티 채널에서 소개

## 리소스

### 1. 문서
- [공식 문서](https://docs.n8n.io/)
- [API 참조](https://docs.n8n.io/api/)
- [노드 개발 가이드](https://docs.n8n.io/hosting/nodes/)

### 2. 도구
- [VS Code](https://code.visualstudio.com/)
- [GitHub Desktop](https://desktop.github.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)

### 3. 학습 자료
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Vue.js Guide](https://vuejs.org/guide/)
- [Express.js Guide](https://expressjs.com/en/guide/)

## 감사합니다!

n8n 프로젝트에 관심을 가져주셔서 감사합니다. 여러분의 기여가 n8n을 더 나은 프로젝트로 만들어갑니다!

### 궁금한 점이 있으신가요?
- GitHub Issues에 질문 남기기
- Discord에서 실시간 문의
- 메일: contributions@n8n.io

기여 과정에서 어려움이 있거나 도움이 필요하시면 언제든지 문의해주세요.