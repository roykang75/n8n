# Vue.js 애플리케이션 구조

n8n 프론트엔드는 Vue 3, TypeScript, Vite를 기반으로 현대적인 SPA(Single Page Application)로 구축되었습니다. 이 문서는 전체 아키텍처와 구성 요소를 상세히 설명합니다.

## 애플리케이션 시작점

### main.ts - 애플리케이션 초기화

```typescript
import { createApp } from 'vue';
import App from '@/app/App.vue';
import router from '@/app/router';
import { i18nInstance } from '@n8n/i18n';
import { createPinia, PiniaVuePlugin } from 'pinia';

const pinia = createPinia();
const app = createApp(App);

// 플러그인 등록 순서가 중요
app.use(SentryPlugin);
app.use(TelemetryPlugin);
app.use(PiniaVuePlugin);
app.use(pinia);
app.use(router);
app.use(i18nInstance);

app.mount('#app');
```

**초기화 플러그인들:**
- **SentryPlugin**: 에러 추적 및 성능 모니터링
- **TelemetryPlugin**: 사용자 행동 추적
- **Pinia**: 상태 관리
- **Router**: 라우팅 관리
- **i18n**: 다국어 지원

### 애플리케이션 구조

```
src/
├── app/                          # 애플리케이션 코어
│   ├── App.vue                   # 루트 컴포넌트
│   ├── router.ts                 # 라우터 설정
│   ├── stores/                   # Pinia 스토어들
│   ├── plugins/                  # Vue 플러그인들
│   ├── composables/              # Composition API 유틸리티
│   ├── components/               # 공통 컴포넌트들
│   └── views/                    # 페이지 레벨 컴포넌트
├── features/                     # 기능별 모듈화
│   ├── workflows/                # 워크플로우 관련
│   ├── executions/               # 실행 관련
│   ├── settings/                 # 설정 관련
│   ├── collaborations/           # 협업 기능
│   └── ai/                       # AI 기능 (EE)
├── shared/                       # 공유 컴포넌트 및 유틸
└── assets/                       # 정적 자원
```

## 라우팅 시스템

### Vue Router 구성

```typescript
// router.ts
const router = createRouter({
  history: createWebHistory('/'),
  routes: [
    // 비인증 라우트
    {
      path: '/signin',
      name: 'signin',
      component: SigninView,
      meta: { requiresAuth: false }
    },
    // 인증 필요 라우트
    {
      path: '/workflow/:id',
      name: 'workflow',
      component: WorkflowView,
      meta: {
        requiresAuth: true,
        middleware: ['rbac', 'workflowAccess']
      }
    }
  ]
});
```

### 라우팅 아키텍처 특징

1. **Lazy Loading**: 모든 페이지 컴포넌트는 동적 임포트
2. **Route Guards**: 인증 및 권한 확인
3. **Middleware**: 라우트별 미들웨어 지원
4. **Modular Routes**: 기능별 라우트 분리

### 라우트 미들웨어

```typescript
// rbac 미들웨어 예시
const rbacMiddleware = async (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  const settingsStore = useSettingsStore();

  if (!settingsStore.isEnterpriseEdition && to.meta.eeFeature) {
    next({ name: 'workflow' });
    return;
  }

  next();
};
```

## 상태 관리 (Pinia)

### 스토어 구조

```typescript
// stores/ui.store.ts
import { defineStore } from 'pinia';

export const useUIStore = defineStore('ui', {
  state: () => ({
    sidebarOpen: true,
    theme: 'light',
    modals: {} as Record<string, boolean>
  }),

  getters: {
    isDarkMode: (state) => state.theme === 'dark',
    activeModals: (state) => Object.entries(state.modals)
      .filter(([, isOpen]) => isOpen)
      .map(([name]) => name)
  },

  actions: {
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen;
    },

    openModal(modalName: string) {
      this.modals[modalName] = true;
    },

    closeModal(modalName: string) {
      this.modals[modalName] = false;
    }
  }
});
```

### 주요 스토어들

| 스토어 | 역할 | 주요 상태 |
|------|------|-----------|
| `ui.store` | UI 상태 관리 | 테마, 사이드바, 모달 |
| `settings.store` | 애플리케이션 설정 | 사용자 설정, 환경 설정 |
| `workflows.store` | 워크플로우 관리 | 워크플로우 목록, 상태 |
| `nodeTypes.store` | 노드 타입 관리 | 사용 가능한 노드들 |
| `users.store` | 사용자 관리 | 인증, 권한 정보 |

## 컴포넌트 아키텍처

### 컴포넌트 분류

1. **Pages** (`views/`): 라우트와 직접 연결되는 컴포넌트
2. **Layouts** (`layouts/`): 페이지 구조를 잡는 컴포넌트
3. **Features** (`features/`): 특정 기능을 담당하는 컴포넌트
4. **Shared** (`shared/`): 재사용 가능한 순수 컴포넌트
5. **App** (`app/components/`): 애플리케이션 레벨 공통 컴포넌트

### 컴포넌트 예시

```vue
<!-- WorkflowCanvas.vue -->
<template>
  <div class="workflow-canvas">
    <NodeToolbar />
    <VueFlow
      :nodes="nodes"
      :edges="connections"
      @node-click="onNodeClick"
      @connect="onConnection"
    >
      <Controls />
      <MiniMap />
      <Background />
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { computed } from 'vue';
import { useWorkflowsStore } from '@/stores/workflows.store';

const workflowsStore = useWorkflowsStore();
const { onNodeClick, onConnection } = useWorkflowInteractions();

const nodes = computed(() => workflowsStore.nodes);
const connections = computed(() => workflowsStore.connections);
</script>
```

## Composition API 사용 패턴

### Composables 구조

```typescript
// composables/useWorkflowInteractions.ts
import { ref, computed } from 'vue';
import { useWorkflowsStore } from '@/stores/workflows.store';
import type { INode } from '@n8n/api-types';

export function useWorkflowInteractions() {
  const workflowsStore = useWorkflowsStore();
  const selectedNode = ref<INode | null>(null);

  const hasUnsavedChanges = computed(() =>
    workflowsStore.workflow.name !== workflowsStore.originalWorkflow.name
  );

  const onNodeClick = (event: { node: INode }) => {
    selectedNode.value = event.node;
    workflowsStore.selectNode(event.node.id);
  };

  const onConnection = (connection: any) => {
    workflowsStore.addConnection(connection);
  };

  return {
    selectedNode,
    hasUnsavedChanges,
    onNodeClick,
    onConnection
  };
}
```

### 주요 Composables

| Composable | 역할 |
|------------|------|
| `useExternalHooks` | 외부 훅 관리 |
| `useTelemetry` | 원격 측정 |
| `useTheme` | 테마 관리 |
| `useNodeHelpers` | 노드 조작 헬퍼 |
| `useWorkflowsStore` | 워크플로우 스토어 접근 |

## TypeScript 적용

### 타입 정의 구조

```typescript
// types/workflow.ts
export interface IWorkflow {
  id: string;
  name: string;
  nodes: INode[];
  connections: IConnections;
  active: boolean;
  settings: IWorkflowSettings;
  tags?: ITag[];
}

export interface INode {
  id: string;
  name: string;
  type: string;
  typeVersion: number;
  position: [number, number];
  parameters: IDataObject;
  webhookId?: string;
  disabled?: boolean;
}
```

### 컴포넌트 타입 안전성

```vue
<script setup lang="ts">
interface Props {
  workflow: IWorkflow;
  readonly?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false
});

const emit = defineEmits<{
  update: [workflow: IWorkflow];
  save: [];
}>();
</script>
```

## i18n (국제화)

### 번역 구조

```typescript
// @n8n/i18n
export interface ITranslations {
  'workflow.name': string;
  'node.title': { nodeName: string };
  'error.required': { field: string };
}
```

### 컴포넌트에서 사용

```vue
<template>
  <div>
    <h1>{{ $t('workflow.name') }}</h1>
    <p>{{ $t('node.title', { nodeName: node.name }) }}</p>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
</script>
```

## 빌드 시스템 (Vite)

### Vite 설정

```typescript
// vite.config.ts
export default defineConfig({
  plugins: [
    vue(),
    dts({ include: ['src/**/*'] }),
    // ...
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['vue', 'vue-router', 'pinia'],
          'editor': ['@vue-flow/core', '@vue-flow/background'],
          'ui': ['@n8n/design-system']
        }
      }
    }
  },
  define: {
    __VUE_I18N_FULL_INSTALL__: true,
    __VUE_I18N_LEGACY_API__: false
  }
});
```

### 최적화 전략

1. **Code Splitting**: 라우트 및 기능별 분리
2. **Tree Shaking**: 사용되지 않는 코드 제거
3. **Asset Optimization**: 이미지 및 폰트 최적화
4. **Vendor Chunks**: 반 vendor 라이브러리 분리

## 테스트 전략 (Vitest)

### 테스트 구성

```typescript
// vitest.config.ts
export default defineConfig({
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov']
    }
  }
});
```

### 컴포넌트 테스트 예시

```typescript
// MyComponent.spec.ts
import { mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import MyComponent from './MyComponent.vue';

describe('MyComponent', () => {
  it('renders correctly', () => {
    const wrapper = mount(MyComponent, {
      global: {
        plugins: [createPinia()]
      }
    });

    expect(wrapper.find('h1').text()).toBe('Hello World');
  });
});
```

## 디자인 시스템 연동

### @n8n/design-system 사용

```vue
<template>
  <div>
    <n8n-button
      type="primary"
      :loading="isLoading"
      @click="onSave"
    >
      {{ $t('save') }}
    </n8n-button>

    <n8n-input
      v-model="value"
      :placeholder="$t('placeholder')"
      :validation-error="error"
    />

    <n8n-modal
      v-model="isModalOpen"
      :title="$t('modal.title')"
    >
      <p>{{ $t('modal.content') }}</p>
    </n8n-modal>
  </div>
</template>
```

### CSS 변수 활용

```scss
.my-component {
  padding: var(--spacing--md);
  background-color: var(--color--background);
  border: var(--border) var(--color--foreground);
  border-radius: var(--radius);
}
```

## 성능 최적화

### 1. 컴포넌트 최적화

```vue
<script setup lang="ts">
import { defineAsyncComponent, shallowRef } from 'vue';

// 무거운 컴포넌트는 비동기 로드
const HeavyComponent = defineAsyncComponent(() =>
  import('./HeavyComponent.vue')
);

// 큰 객체는 얕은 반응성 사용
const largeData = shallowRef({});
</script>
```

### 2. 가상 스크롤

```vue
<template>
  <n8n-virtual-list
    :items="largeList"
    :item-height="48"
    v-slot="{ item }"
  >
    <ListItem :item="item" />
  </n8n-virtual-list>
</template>
```

### 3. 메모이제이션

```typescript
import { computed, memoize } from 'vue';

const expensiveComputation = memoize((data: any[]) => {
  return data.reduce(/* 복잡한 연산 */);
});
```

## 에러 핸들링

### 전역 에러 핸들러

```typescript
main.ts
app.config.errorHandler = (err, instance, info) => {
  console.error('Global error:', err);
  Sentry.captureException(err);
};
```

### 컴포넌트 에러 경계

```vue
<template>
  <ErrorBoundary @error="onError">
    <MyComponent />
  </ErrorBoundary>
</template>
```

## 개발 도구

### 1. Vue DevTools
- 컴포넌트 트리 확인
- 상태 관리 디버깅
- 성능 프로파일링

### 2. TypeScript
- IDE 자동 완성
- 컴파일 타임 에러 체크
- 리팩토링 지원

### 3. Vite HMR
- 빠른 개발 사이클
- 상태 유지 리로드
- 모듈 핫 리플레이스먼트

## 모범 사례

1. **컴포넌트 설계**
   - 단일 책임 원칙 준수
   - Props/Events 인터페이스 명확화
   - 재사용성 고려

2. **상태 관리**
   - 로컬 상태优先 사용
   - 전역 상태 최소화
   - 스토어 모듈화

3. **성능**
   - 지연 로딩 활용
   - 불필요한 리렌더링 방지
   - 메모이제이션 적극 활용

4. **타입 안전성**
   - strict 모드 사용
   - 타입 단언 최소화
   - 공용 타입 정의