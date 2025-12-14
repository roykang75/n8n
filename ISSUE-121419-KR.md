# ISSUE-121419: 워크플로우 생성 오류 분석 보고서

## 1. 이슈 설명
**오류 메시지:** `java.lang.ClassCastException: class java.util.LinkedHashMap cannot be cast to class java.lang.String`
**엔드포인트:** `POST /rest/workflows`
**시나리오:** 표준 페이로드( `settings` 및 `meta` 객체 포함)로 새 워크플로우 생성 시 발생.
**영향:** 워크플로우 생성 및 저장 불가, 핵심 기능 차단.

## 2. 원인 분석
초기 조사에서는 JSON 객체(Jackson이 `LinkedHashMap`으로 파싱)가 Java 백엔드에서 `String` 필드로 잘못 캐스팅되는 JSON 역직렬화 문제로 판단했습니다.

그러나 광범위한 리팩토링과 테스트 결과, **실행 중인 애플리케이션에 최신 코드 변경 사항이 반영되지 않고 있음**이 강력하게 시사됩니다. 이 특정 오류를 우회하기 위해 수많은 코드 수정(예: 필드를 `Object`로 변경, 기본값 강제 적용 등)을 적용했음에도 불구하고 오류는 동일하게 지속되며, 코드베이스에 추가한 디버그 로그가 출력에 나타나지 않습니다.

## 3. 상세 시도 내역

### 시도 1: DTO 리팩토링 (초기 수정)
- **조치:** `WorkflowController`의 수동 `Map<String, Object>` 파싱을 강력한 타입의 `CreateWorkflowRequest` DTO로 대체했습니다.
- **목표:** Jackson의 자동 역직렬화 처리 활용.
- **결과:** 실패. 오류 지속됨.

### 시도 2: 향상된 DTO 설정
- **조치:** 모든 DTO 및 내부 클래스(`WorkflowSettings`, `WorkflowMeta`)에 `@JsonIgnoreProperties(ignoreUnknown = true)`를 추가했습니다.
- **조치:** `tags` 필드를 `@JsonProperty("tags")`로 명시적으로 매핑하고 `List<Object>`(Map/String 혼합 타입) 처리 로직을 추가했습니다.
- **목표:** 프론트엔드에서의 스키마 불일치 또는 추가 필드 처리.
- **결과:** 실패. 오류 지속됨.

### 시도 3: 타입 안전성 완화 (`Object` 사용)
- **조치:** `CreateWorkflowRequest`의 `settings` 및 `meta` 필드를 구체적인 타입에서 `Object`로 변경했습니다.
- **조치:** `toServiceRequest()` 내부에서 `ObjectMapper.convertValue()`를 사용한 수동 변환을 구현했습니다.
- **목표:** Jackson이 초기 바인딩 단계에서 암시적 캐스팅을 시도하는 것을 방지.
- **결과:** 실패. 오류 지속됨.

### 시도 4: 디버그 로깅 및 검증
- **조치:** `WorkflowController` 진입점에 명시적 로그(`VERSION 2`, `VERSION 3`) 및 `System.out.println`을 추가했습니다.
- **목표:** 새 코드가 실행되고 있는지 확인.
- **관찰:** **로그가 출력에 나타나지 않았습니다.** 표준 Spring Boot 시작 로그만 표시되었습니다.

### 시도 5: 하드코딩 우회 (기본값 강제)
- **조치:** 들어오는 `settings` 및 `meta` 페이로드를 완전히 무시하고 `new WorkflowSettings()`(기본 Java 객체)를 강제하도록 코드를 수정했습니다.
- **목표:** 들어오는 데이터 구조가 문제의 원인인지 증명.
- **결과:** **실패. 오류가 정확히 지속됨.** 만약 새 코드가 실행 중이었다면, 문제가 되는 페이로드 데이터를 더 이상 건드리지 않으므로 이는 논리적으로 불가능합니다.

### 시도 6: 컨트롤러 시그니처 원복
- **조치:** `WorkflowController.createWorkflow` 시그니처를 다시 `Map<String, Object>`를 받도록 되돌리고, DTO 변환을 메서드 내부로 이동했습니다.
- **목표:** 잠재적인 Spring MVC Argument Resolver 문제 우회.
- **결과:** 실패.

## 4. 주요 관찰 사항
1.  **단순 페이로드로 부분 성공:** `settings` 필드가 없는 요청(`{"name": "Simple"}`)은 성공합니다. 이는 레거시 코드(또는 숨겨진 코드)가 기본 필드는 처리하지만 `settings`와 같은 복잡한 객체에서 충돌함을 의미합니다.
2.  **`Simple Test` 참조:** 서버가 응답하며 단순 케이스에 대해 데이터베이스 쓰기가 작동함을 확인했습니다.
3.  **로그 누락:** 디버그 로그(`VERSION X`)가 나타나지 않아, 실행 중인 바이트코드가 소스 코드와 일치하지 않음을 확인함.

## 5. 결론 및 권장 사항
개발 환경(Gradle 빌드/캐시 또는 실행 프로세스)이 수정된 `WorkflowController` 클래스를 효과적으로 재패키징하지 못하고 있거나, `bootRun`/`java -jar`가 캐시된 아티팩트를 제공하고 있는 상태에 갇혀 있습니다.

**권장 조치:**
```bash
    ./gradlew clean --no-daemon
    rm -rf springboot/build
    ```
2.  **표준 스크립트를 통한 재시작:**
    제공된 `start-dev.sh`를 사용하여 올바른 빌드 라이프사이클이 트리거되도록 하십시오.
    ```bash
    ./start-dev.sh
    ```

## 6. 추가 디버깅 시도 (2025-12-14)

### 시도 7: Clean Build 및 재시작
- **조치:** Clean build 수행 및 Docker 실행 확인
- **결과:** 실패. 동일한 에러 발생
- **발견:** 컨트롤러 메서드가 **여전히** 실행되지 않음 (VERSION 로그 없음)

### 시도 8: Authentication 파라미터 제거
- **조치:** `Authentication` 파라미터를 제거하고 Mock User 사용
- **결과:** 컨트롤러 메서드가 실행됨! 하지만 H엔티티티berna에서 TransientObjectException 발생
- **결론:** `Authentication` 파라미터가 문제를 일으키고 있음

### 시도 9: @AuthenticationPrincipal 사용
- **조치:** `Authentication` 대신 `@AuthenticationPrincipal UserDetails` 사용
- **결과:** 실패. 여전히 동일한 `LinkedHashMap cannot be cast to String` 에러
- **결론:** `@AuthenticationPrincipal`도 같은 문제 발생

### 시도 10: SecurityContextHolder 사용
- **조치:** `@AuthenticationPrincipal` 제거하고 `SecurityContextHolder.getContext().getAuthentication()` 사용
- **결과:** 실패. 여전히 동일한 에러
- **결론:** **인증 파라미터와 무관한 문제임을 확인**

### 시도 11: toServiceRequest() 변환 로직 수정
- **조치:** `CreateWorkflowRequest.toServiceRequest()`에서 `ObjectMapper.convertValue()`를 사용하여 `settings`와 `meta`를 올바른 타입으로 변환
- **코드:**
    ```java
    WorkflowEntity.WorkflowSettings settingsObj = settings != null 
        ? mapper.convertValue(settings, WorkflowEntity.WorkflowSettings.class)
        : new WorkflowEntity.WorkflowSettings();
    ```
- **결과:** 실패. 동일한 에러 발생
- **결론:** 컨트롤러에 도달하기 전에 에러가 발생함

## 7. 현재 상태 및 다음 단계

**확인된 사실:**
1. ✅ Clean build 후에도 에러 발생
2. ✅ 컨트롤러 메서드가 실행되지 않음 (VERSION 로그 없음)
3. ✅ 인증 파라미터와 무관 (제거해도 에러 발생)
4. ✅ Spring Boot MVC의 `@RequestBody` deserialization 단계에서 에러 발생

**근본 원인:**
Spring이 `POST /rest/workflows`의 `@RequestBody Map<String, Object>`를 Jackson으로 deserialize할 때, 
`settings` 필드 (JSON 객체)를 처리하는 과정에서 어딘가에서 String으로 casting하려는 문제가 발생.

**다음 단계:**
1. 스택 트레이스의 정확한 시작 지점 확인하여 에러 발생 위치 특정
2. Jackson deserialization 로직 디버깅
3. 가능하면 `@RequestBody`를 `String`으로 받아서 수동으로 파싱하는 임시 workaround 고려
```
