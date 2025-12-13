# 노드 아키텍처

n8n의 노드 시스템은 워크플로우의 기본 빌딩 블록으로, 각 노드는 특정 기능이나 서비스와의 통합을 담당합니다. 이 문서는 노드 아키텍처와 개발 방법을 상세히 설명합니다.

## 노드의 개념

노드는 워크플로우에서 개별 작업을 수행하는 단위입니다. 각 노드는:
- 입력 데이터를 받아 처리
- 특정 로직 수행 (API 호출, 데이터 변환 등)
- 처리 결과를 다음 노드로 전달

## 노드 구조

### 1. 기본 파일 구조

```
MyNode/
├── MyNode.node.ts           # 메인 노드 정의
├── MyNode.node.json         # 노드 메타데이터
├── GenericFunctions.ts      # 공통 함수들
├── icons/
│   ├── mynode.svg           # 라이트 모드 아이콘
│   └── mynode.dark.svg      # 다크 모드 아이콘
├── operations/              # 오퍼레이션별 정의
│   ├── item.ts
│   └── folder.ts
├── V1/                      # 버전 1 구현
│   └── MyNodeV1.node.ts
├── V2/                      # 버전 2 구현
│   └── MyNodeV2.node.ts
└── test/                    # 테스트 파일
    ├── MyNode.test.ts
    └── workflows/
        └── test.workflow.json
```

### 2. 노드 클래스 정의

```typescript
// MyNode.node.ts
import type { INodeType, INodeTypeDescription } from 'n8n-workflow';

export class MyNode implements INodeType {
  description: INodeTypeDescription = {
    displayName: 'My Service',
    name: 'myService',
    icon: 'file:mynode.svg',
    group: ['transform'],
    version: 1,
    subtitle: '={{$parameter["operation"] + ": " + $parameter["resource"]}}',
    description: 'Interacts with My Service API',
    defaults: {
      name: 'My Service',
    },
    inputs: ['main'],
    outputs: ['main'],
    credentials: [
      {
        name: 'myServiceApi',
        required: true,
      },
    ],
    requestDefaults: {
      baseUrl: 'https://api.myservice.com/v1',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
    },
    properties: [
      {
        displayName: 'Operation',
        name: 'operation',
        type: 'options',
        noDataExpression: true,
        options: [
          {
            name: 'Get',
            value: 'get',
            description: 'Get an item',
            action: 'Get an item',
          },
          {
            name: 'Create',
            value: 'create',
            description: 'Create an item',
            action: 'Create an item',
          },
        ],
        default: 'get',
      },
      // ... 추가 속성들
    ],
  };

  async execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
    const items = this.getInputData();
    const operation = this.getNodeParameter('operation', 0) as string;

    let responseData: any[] = [];

    if (operation === 'get') {
      responseData = await this.getAll(items);
    } else if (operation === 'create') {
      responseData = await this.create(items);
    }

    return this.helpers.returnJsonArray(responseData);
  }
}
```

### 3. 메소드 구현

```typescript
// GenericFunctions.ts
import type { IExecuteFunctions, IDataObject } from 'n8n-workflow';

export async function getAll(
  this: IExecuteFunctions,
  items: INodeExecutionData[]
): Promise<IDataObject[]> {
  const returnData: IDataObject[] = [];

  for (let i = 0; i < items.length; i++) {
    const resourceId = this.getNodeParameter('resourceId', i) as string;

    const response = await this.helpers.httpRequestWithAuthentication.call(
      this,
      'myServiceApi',
      {
        method: 'GET',
        url: `/items/${resourceId}`,
        json: true,
      }
    );

    returnData.push(response);
  }

  return returnData;
}

export async function create(
  this: IExecuteFunctions,
  items: INodeExecutionData[]
): Promise<IDataObject[]> {
  const returnData: IDataObject[] = [];

  for (let i = 0; i < items.length; i++) {
    const body = this.getNodeParameter('body', i) as IDataObject;

    const response = await this.helpers.httpRequestWithAuthentication.call(
      this,
      'myServiceApi',
      {
        method: 'POST',
        url: '/items',
        body,
        json: true,
      }
    );

    returnData.push(response);
  }

  return returnData;
}
```

## 노드 타입

### 1. 프로그램매틱 노드 (Programmatic Nodes)

직접 `execute` 함수를 구현하여 로직을 처리합니다.

```typescript
// Discord.node.ts
export class Discord implements INodeType {
  // ... description

  async execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
    const items = this.getInputData();
    const operation = this.getNodeParameter('operation', 0);

    switch (operation) {
      case 'sendMessage':
        return await sendMessage.call(this, items);
      case 'getChannel':
        return await getChannel.call(this, items);
      default:
        throw new NodeOperationError(this.getNode(), `Unknown operation: ${operation}`);
    }
  }
}
```

### 2. 선언적 노드 (Declarative Nodes)

`requestDefaults`와 라우팅 설정을 사용하여 API 호출을 정의합니다.

```typescript
// Okta.node.ts
export class Okta implements INodeType {
  description: INodeTypeDescription = {
    // ... 기본 설정
    requestDefaults: {
      baseUrl: 'https://{{$credentials.oktaApi.domain}}/api/v1',
      headers: {
        'Authorization': 'SSWS {{$credentials.oktaApi.apiToken}}',
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
    },
    routing: {
      request: {
        getOperations: [
          {
            name: 'Get Users',
            displayName: 'Get Users',
            description: 'Retrieves all users',
            request: {
              method: 'GET',
              url: '/users',
            },
            output: {
              postReceive: [
                {
                  type: 'main',
                  index: 0,
                },
              ],
            },
          },
        ],
      },
    },
  };
}
```

## 트리거 노드

### 1. 웹훅 트리거

```typescript
// MicrosoftTeamsTrigger.node.ts
export class MicrosoftTeamsTrigger implements INodeType {
  description: INodeTypeDescription = {
    // ... 설정
    webhook: true,
  };

  async webhook(this: IWebhookFunctions): Promise<IWebhookResponseData> {
    const req = this.getRequestObject();

    return {
      workflowData: [
        {
          json: req.body,
        },
      ],
    };
  }

  async webhookCheckExists(this: IWebhookFunctions): Promise<boolean> {
    const webhookUrl = this.getNodeWebhookUrl('default');
    const webhookData = await this.helpers.httpRequestWithAuthentication.call(
      this,
      'microsoftTeamsApi',
      {
        method: 'GET',
        url: `/subscriptions?webhookUrl=${webhookUrl}`,
      }
    );

    return webhookData.value.length > 0;
  }

  async webhookCreate(this: IWebhookFunctions): Promise<boolean> {
    // 웹훅 생성 로직
    return true;
  }

  async webhookDelete(this: IWebhookFunctions): Promise<boolean> {
    // 웹훅 삭제 로직
    return true;
  }
}
```

### 2. 폴링 트리거

```typescript
// GmailTrigger.node.ts
export class GmailTrigger implements INodeType {
  description: INodeTypeDescription = {
    // ... 설정
    polling: true,
  };

  async poll(this: IPollFunctions): Promise<INodeExecutionData[][]> {
    const nodeStaticData = this.getWorkflowStaticData('node');

    // 마지막 체크 시간 가져오기
    const lastChecked = nodeStaticData.lastChecked || new Date(0);

    const response = await this.helpers.httpRequestWithAuthentication.call(
      this,
      'gmailApi',
      {
        method: 'GET',
        url: '/messages',
        qs: {
          q: `after:${lastChecked.getTime() / 1000}`,
        },
      }
    );

    // 다음 폴링을 위해 마지막 체크 시간 업데이트
    nodeStaticData.lastChecked = new Date();

    return this.helpers.returnJsonArray(response.messages);
  }
}
```

## 버전 관리

### 1. 라이트 버전 관리

```typescript
export class Set implements INodeType {
  description: INodeTypeDescription = {
    displayName: 'Set',
    name: 'set',
    version: [3, 3.1, 3.2],
    // 버전별 메타데이터 설정
  };

  execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
    const version = this.getNode().typeVersion;

    if (version === 1) {
      // V1 로직
    } else if (version >= 2) {
      // V2+ 로직
    }
  }
}
```

### 2. 풀 버전 관리

```typescript
// Set.node.ts
export class Set extends VersionedNodeType {
  constructor() {
    const baseDescription: INodeTypeBaseDescription = {
      displayName: 'Set',
      name: 'set',
      group: ['transform'],
      defaultVersion: 4.1,
    };

    const nodeVersions = {
      1: new SetV1(baseDescription),
      2: new SetV2(baseDescription),
      3: new SetV3(baseDescription),
      4.1: new SetV4(baseDescription),
    };

    super(nodeVersions, baseDescription);
  }
}
```

## 파라미터 정의

### 1. 동적 옵션

```typescript
properties: [
  {
    displayName: 'Resource',
    name: 'resource',
    type: 'options',
    noDataExpression: true,
    options: [
      {
        name: 'User',
        value: 'user',
        description: 'User resource',
      },
      {
        name: 'Channel',
        value: 'channel',
        description: 'Channel resource',
      },
    ],
    default: 'user',
  },
  {
    displayName: 'Operation',
    name: 'operation',
    type: 'options',
    noDataExpression: true,
    displayOptions: {
      show: {
        resource: ['user'],
      },
    },
    options: [
      {
        name: 'Get',
        value: 'get',
        description: 'Get user',
      },
      {
        name: 'Create',
        value: 'create',
        description: 'Create user',
      },
    ],
    default: 'get',
  },
]
```

### 2. 동적 로드 옵션

```typescript
properties: [
  {
    displayName: 'List ID',
    name: 'listId',
    type: 'options',
    typeOptions: {
      loadOptionsMethod: 'getListIds',
    },
    default: '',
  },
  // ...
],
methods: {
  loadOptions: {
    async getListIds(this: ILoadOptionsFunctions): Promise<INodePropertyOptions[]> {
      const data = await this.helpers.httpRequestWithAuthentication.call(
        this,
        'asanaApi',
        {
          method: 'GET',
          url: '/projects',
          json: true,
        }
      );

      return data.data.map((project: any) => ({
        name: project.name,
        value: project.gid,
      }));
    },
  },
}
```

### 3. 리소스 로케이터

```typescript
properties: [
  {
    displayName: 'File',
    name: 'file',
    type: 'resourceLocator',
    default: {
      mode: 'list',
      url: '',
    },
    modes: [
      {
        displayName: 'From List',
        value: 'list',
        description: 'Select from the list',
      },
      {
        displayName: 'Enter URL',
        value: 'url',
        description: 'Enter the URL manually',
      },
    ],
    typeOptions: {
      searchListMethod: 'searchFiles',
      searchFilterRequired: true,
      searchEnabled: true,
      filterSearch: true,
      searchFilterPlaceHolder: 'Filter files...',
    },
  },
  // ...
],
methods: {
  loadOptions: {
    async searchFiles(
      this: ILoadOptionsFunctions,
      filter?: string
    ): Promise<INodePropertyOptions[]> {
      const query: IDataObject = {};
      if (filter) {
        query.include_archived = false;
        query.include_deleted = false;
        query.name = filter;
      }

      const data = await this.helpers.httpRequestWithAuthentication.call(
        this,
        'dropboxApi',
        {
          method: 'POST',
          url: '/2/files/search/v2',
          body: {
            query: filter || '',
          },
          json: true,
        }
      );

      return data.matches.map((match: any) => ({
        name: match.metadata.name,
        value: match.metadata.path_display,
        description: match.metadata.path_lower,
      }));
    },
  },
}
```

## 크리더셜

### 1. API 키 인증

```typescript
// credentials/MyServiceApi.credentials.ts
import type {
  ICredentialTestRequest,
  INodeProperties,
} from 'n8n-workflow';

export class MyServiceApi implements ICredentialType {
  name = 'myServiceApi';
  displayName = 'My Service API';
  properties: INodeProperties[] = [
    {
      displayName: 'API Key',
      name: 'apiKey',
      type: 'string',
      typeOptions: {
        password: true,
      },
      default: '',
      required: true,
      description: 'API Key for My Service',
    },
  ];

  authenticate: {
    type: 'generic',
    properties: {
      authentication: 'predefinedCredentialType',
      predefinedCredentialType: 'httpHeaderAuth',
    },
  } = {
    type: 'generic',
    properties: {
      authentication: 'predefinedCredentialType',
      predefinedCredentialType: 'httpHeaderAuth',
    },
  };

  test: ICredentialTestRequest = {
    request: {
      baseURL: 'https://api.myservice.com/v1',
      url: '/me',
    },
  };
}
```

### 2. OAuth2 인증

```typescript
export class GoogleOAuth2Api implements ICredentialType {
  name = 'googleOAuth2Api';
  properties: INodeProperties[] = [
    {
      displayName: 'Grant Type',
      name: 'grantType',
      type: 'hidden',
      typeOptions: {
        // ...
      },
      default: 'authorizationCode',
    },
    {
      displayName: 'Client ID',
      name: 'clientId',
      type: 'string',
      required: true,
    },
    {
      displayName: 'Client Secret',
      name: 'clientSecret',
      type: 'string',
      typeOptions: {
        password: true,
      },
      required: true,
    },
    // ...
  ];

  authenticate: {
    type: 'oauth2',
    grantType: 'authorizationCode',
    authUrl: 'https://accounts.google.com/o/oauth2/auth',
    accessTokenUrl: 'https://oauth2.googleapis.com/token',
    auth: {
      query: {
        access_type: 'offline',
        prompt: 'consent',
      },
    },
  } = {
    type: 'oauth2',
    grantType: 'authorizationCode',
    authUrl: 'https://accounts.google.com/o/oauth2/auth',
    accessTokenUrl: 'https://oauth2.googleapis.com/token',
    auth: {
      query: {
        access_type: 'offline',
        prompt: 'consent',
      },
    },
  };
}
```

## 테스트

### 1. 단위 테스트

```typescript
// MyNode.test.ts
import { mock } from 'jest-mock-extended';
import nock from 'nock';
import { MyNode } from '../MyNode.node';

describe('MyNode', () => {
  let node: MyNode;
  const executeFunctions = mock<IExecuteFunctions>();

  beforeEach(() => {
    node = new MyNode();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('execute', () => {
    it('should get items successfully', async () => {
      // Setup
      const items = [{ json: { resourceId: '123' } }];
      executeFunctions.getInputData.mockReturnValue(items);
      executeFunctions.getNodeParameter.mockReturnValue('get');

      nock('https://api.myservice.com/v1')
        .get('/items/123')
        .reply(200, { id: '123', name: 'Test Item' });

      // Execute
      const result = await node.execute.call(executeFunctions);

      // Verify
      expect(result).toHaveLength(1);
      expect(result[0][0].json).toEqual({
        id: '123',
        name: 'Test Item',
      });
    });

    it('should handle API errors', async () => {
      // Setup
      const items = [{ json: {} }];
      executeFunctions.getInputData.mockReturnValue(items);
      executeFunctions.getNodeParameter.mockReturnValue('get');

      nock('https://api.myservice.com/v1')
        .get('/items/undefined')
        .reply(404, { error: 'Not found' });

      // Execute & Verify
      await expect(node.execute.call(executeFunctions)).rejects.toThrow();
    });
  });
});
```

### 2. 워크플로우 테스트

```typescript
// workflows/getItem.workflow.json
{
  "name": "Get Item Test",
  "nodes": [
    {
      "name": "MyNode",
      "type": "n8n-nodes-base.myNode",
      "typeVersion": 1,
      "position": [240, 300],
      "parameters": {
        "operation": "get",
        "resourceId": "123"
      }
    }
  ],
  "connections": {}
}

// MyNode.test.ts
import { NodeTestHarness } from '@n8n/testing';

describe('MyNode Workflow Tests', () => {
  let testHarness: NodeTestHarness;

  beforeAll(async () => {
    testHarness = new NodeTestHarness(__dirname, 'item.workflow.json');
  });

  beforeEach(() => {
    nock.cleanAll();
  });

  it('should execute workflow successfully', async () => {
    // Setup mock
    nock('https://api.myservice.com/v1')
      .get('/items/123')
      .reply(200, { id: '123', name: 'Test Item' });

    // Execute
    const result = await testHarness.runWorkflow('MyNode');

    // Verify
    expect(result.resultData.runData).toHaveLength(1);
    expect(result.resultData.runData['MyNode'][0].data.main[0].json).toEqual({
      id: '123',
      name: 'Test Item',
    });
  });
});
```

## 모범 사례

### 1. 코드 구성
- 관심사 분리: 오퍼레이션별로 파일 분리
- 재사용 가능한 함수: GenericFunctions에 공통 로직
- 타입 안전성: 명확한 TypeScript 타입 사용

### 2. 에러 처리
```typescript
try {
  const response = await this.helpers.httpRequestWithAuthentication.call(
    this,
    'myServiceApi',
    options
  );
  return response;
} catch (error) {
  if (error.statusCode === 404) {
    throw new NodeOperationError(
      this.getNode(),
      'Item not found',
      { itemIndex: i }
    );
  }
  throw new NodeApiError(this.getNode(), error as HttpResponse);
}
```

### 3. 데이터 처리
- 입력 데이터 유효성 검사
- 출력 데이터 정규화
- 바이너리 데이터 적절한 처리

### 4. 성능 최적화
- 불필요한 API 호출 최소화
- 데이터 페이지네이션 지원
- 캐싱 활용

## 커뮤니티 노드 개발

### 1. 노드 생성 CLI
```bash
# n8n 노드 생성 도구 사용
npx n8n-node-dev create my-node

# 수동으로 스캐폴딩
mkdir n8n-nodes-myservice
cd n8n-nodes-myservice
npm init -y
# ...
```

### 2. 패키지 구조
```
n8n-nodes-myservice/
├── package.json
├── tsconfig.json
├── src/
│   ├── credentials/
│   │   └── MyServiceApi.credentials.ts
│   └── nodes/
│       └── MyService/
│           └── MyService.node.ts
├── dist/
└── README.md
```

### 3. 배포
```bash
npm run build
npm publish
```

### 4. n8n에서 사용
```bash
npm install n8n-nodes-myservice
n8n start
```

이렇게 하면 커뮤니티 노드가 자동으로 로드되어 노드 팔레트에 나타납니다.