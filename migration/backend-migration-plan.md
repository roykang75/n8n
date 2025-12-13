# n8n Backend Migration Plan: Node.js to Spring Boot 3.5

## Overview

본 문서는 n8n의 백엔드를 Node.js/TypeScript에서 Spring Boot 3.5로 마이그레이션하는 전략을 상세히 설명합니다. 프론트엔드는 수정하지 않고 백엔드만 Spring Boot, JDK 21, Gradle, MySQL 8, Redis로 전환하는 것을 목표로 합니다.

## Target Technology Stack

- **Framework**: Spring Boot 3.5
- **Language**: Java (JDK 21)
- **Build Tool**: Gradle
- **Database**: MySQL 8
- **Cache**: Redis
- **ORM**: Spring Data JPA + Hibernate
- **Migration**: Flyway
- **Testing**: JUnit 5 + Testcontainers

## 1. Project Structure Design

```
springboot/
├── src/
│   ├── main/
│   │   ├── java/xyz/oiio/n8n/
│   │   │   ├── N8nApplication.java
│   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── DatabaseConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   └── AsyncConfig.java
│   │   │   ├── controller/      # REST API controllers (mapped to /rest/...)
│   │   │   │   ├── WorkflowController.java
│   │   │   │   ├── ExecutionController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── CredentialController.java
│   │   │   │   └── NodeController.java
│   │   │   ├── service/         # Business logic
│   │   │   │   ├── workflow/
│   │   │   │   │   ├── WorkflowService.java
│   │   │   │   │   └── WorkflowValidationService.java
│   │   │   │   ├── execution/
│   │   │   │   │   ├── ExecutionService.java
│   │   │   │   │   ├── WorkflowRunner.java
│   │   │   │   │   └── NodeExecutor.java
│   │   │   │   ├── user/
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   └── AuthService.java
│   │   │   │   └── credential/
│   │   │   │       └── CredentialService.java
│   │   │   ├── repository/      # JPA repositories
│   │   │   │   ├── WorkflowRepository.java
│   │   │   │   ├── ExecutionRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── CredentialRepository.java
│   │   │   ├── entity/          # Database entities
│   │   │   │   ├── Workflow.java
│   │   │   │   ├── WorkflowExecution.java
│   │   │   │   ├── User.java
│   │   │   │   ├── Credential.java
│   │   │   │   └── BaseTimeEntity.java
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── workflow/        # Workflow engine
│   │   │   │   ├── model/
│   │   │   │   ├── execution/
│   │   │   │   └── validation/
│   │   │   ├── node/            # Node system
│   │   │   │   ├── base/
│   │   │   │   ├── impl/
│   │   │   │   └── registry/
│   │   │   ├── execution/       # Execution management
│   │   │   │   ├── runner/
│   │   │   │   ├── scheduler/
│   │   │   │   └── executor/
│   │   │   ├── security/        # Authentication/Authorization
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── SecurityFilter.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   └── exception/       # Exception handling
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── WorkflowException.java
│   │   │       └── ExecutionException.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-prod.yml
│   │       ├── db/
│   │       │   └── migration/   # Flyway migrations
│   │       │       ├── V1__Create_User_Table.sql
│   │       │       ├── V2__Create_Workflow_Table.sql
│   │       │       └── V3__Create_Credential_Table.sql
│   │       ├── templates/
│   │       └── static/
│   └── test/
│       └── java/xyz/oiio/n8n/
│           ├── integration/
│           ├── unit/
│           └── testcontainers/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## 2. Gradle Configuration

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.0'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'xyz.oiio.n8n'
version = '1.0.0'

java {
    sourceCompatibility = '21'
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-cache'

    // Database
    runtimeOnly 'com.mysql:mysql-connector-j'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'

    // JSON Processing
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'

    // Async Processing
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'io.projectreactor:reactor-core'

    // Utilities
    implementation 'org.apache.commons:commons-lang3'
    implementation 'com.google.guava:guava:32.1.3-jre'

    // Development Tools
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Monitoring
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mysql'
    testImplementation 'com.redis.testcontainers:testcontainers-redis'
    testImplementation 'org.mockito:mockito-core'

    // Integration Testing
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
}

tasks.named('test') {
    useJUnitPlatform()
}

test {
    testcontainers {
        reuse = true
    }
}
```

## 3. Configuration Examples

### Database Configuration
```java
@Configuration
@EnableJpaRepositories(basePackages = "xyz.oiio.n8n.repository")
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(@Value("${spring.datasource.url}") String url,
                               @Value("${spring.datasource.username}") String username,
                               @Value("${spring.datasource.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        config.setLeakDetectionThreshold(60000);
        return new HikariDataSource(config);
    }

    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateOnMigrate(false)
            .load();
    }
}

### Security Configuration
```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/rest/login", "/rest/owner/setup").permitAll()
                .requestMatchers("/rest/health").permitAll()
                .requestMatchers("/rest/public/**").permitAll()
                .anyRequest().authenticated() // Default to authenticated for other requests
            )
            // Disable CSRF for API endpoints, enable for form-based login if applicable
            .csrf(csrf -> csrf.disable())
            // Add JWT filter or other authentication mechanisms here
            // .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            ;
        return http.build();
    }
}
```

### Redis Configuration
```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Jackson2JsonRedisSerializer for value serialization
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        serializer.setObjectMapper(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

### WebSocket Configuration
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WorkflowExecutionHandler(), "/ws/execution")
            .setAllowedOrigins("*")
            .withSockJS();
    }

    @Bean
    public WebSocketHandler workflowExecutionHandler() {
        return new WorkflowExecutionHandler();
    }
}
```

## 4. Migration Strategy

### Phase 1: Foundation Setup
1. Create Spring Boot project structure
2. Configure database connection (MySQL 8)
3. Set up Redis connection
4. Implement basic security
5. Create base entities and repositories

### Phase 2: Core Domain Migration
1. **User Management**
   - User entity and authentication
   - Role-based access control (RBAC)
   - JWT token implementation

2. **Workflow Management**
   - Workflow entity
   - Version management
   - Validation logic

3. **Credential Management**
   - Secure credential storage
   - Encryption/decryption
   - Sharing permissions

### Phase 3: API Implementation
1. **Workflow CRUD API**
   - Create, read, update, delete workflows
   - Versioning and history
   - Import/export functionality

2. **Execution API**
   - Execute workflows
   - Monitor execution status
   - Handle executions history

3. **Node Management API**
   - List available nodes
   - Node parameter validation
   - Custom node support

### Phase 4: Workflow Engine
1. **Execution Engine**
   - Workflow runner implementation
   - Node execution framework
   - Data flow management

2. **Scheduling**
   - Cron-based triggers
   - Polling triggers
   - Webhook triggers

3. **Error Handling**
   - Retry mechanisms
   - Error reporting
   - Failure notifications

### Phase 5: Integration & Testing
1. **Frontend Integration**
   - API compatibility testing
   - WebSocket connectivity
   - Performance testing

2. **Monitoring**
   - Metrics collection (Micrometer)
   - Health checks
   - Distributed tracing

3. **Documentation**
   - API documentation (OpenAPI)
   - Developer guide
   - Deployment guide

## 5. Key Components

### Workflow Execution Engine
```java
@Service
public class WorkflowExecutionService {

    @Async
    public CompletableFuture<ExecutionResult> executeWorkflow(
        Long workflowId, ExecutionContext context) {

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException());

        WorkflowRunner runner = new WorkflowRunner(workflow, context);
        return runner.execute();
    }

    public void executeNode(Long nodeId, ExecutionData data) {
        NodeExecutor executor = nodeExecutorFactory.getExecutor(nodeId);
        executor.execute(data);
    }
}
```

### Node System
```java
public abstract class BaseNodeExecutor {

    public abstract NodeResult execute(NodeExecutionContext context);

    protected void validateParameters(Map<String, Object> parameters) {
        // Parameter validation logic
    }
}

@Component
public class HttpRequestNodeExecutor extends BaseNodeExecutor {

    @Override
    public NodeResult execute(NodeExecutionContext context) {
        // HTTP request implementation
        return NodeResult.success(responseData);
    }
}
```

## 6. Migration Tips

### 1. Gradual Transition
- Run both Node.js and Spring Boot APIs in parallel
- Use nginx as reverse proxy for routing
- Gradually migrate endpoints one by one

### 2. Data Consistency
- Implement data synchronization scripts
- Use version control for data schema changes
- Backup strategy for rollback

### 3. Performance Optimization
- Use connection pooling (HikariCP)
- Implement multi-level caching
- Use async processing for heavy operations
- Optimize database queries with proper indexing

### 4. Security Considerations
- Use Spring Security with JWT
- Implement proper encryption for sensitive data
- Set up HTTPS and proper CORS
- Audit logging for all API calls

### 5. Monitoring & Observability
```java
@Configuration
public class MonitoringConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
}
```

## 7. Database Migration Strategy

### Using Flyway for Schema Management
1. Export existing database schema from n8n
2. Convert to Flyway migration scripts
3. Handle data transformation if needed
4. Test migrations thoroughly

### Example Migration Script
```sql
-- V1__Create_User_Table.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role ENUM('.ADMIN', '.USER') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_email (email)
);
```

## 8. Testing Strategy

### Unit Testing
```java
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @InjectMocks
    private WorkflowService workflowService;

    @Test
    void shouldCreateWorkflow() {
        // Test implementation
    }
}
```

### Integration Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WorkflowControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldExecuteWorkflow() {
        // Integration test
    }
}
```

## 9. Deployment Considerations

### Docker Configuration
```dockerfile
FROM openjdk:21-jdk-slim

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  n8n-backend:
    build: ./springboot
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: n8n
      MYSQL_ROOT_PASSWORD: rootpw

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

## 10. Rollback Plan

1. Database backups before each migration phase
2. Maintain Node.js version running on different port
3. Use feature flags for gradual rollout
4. Monitor key metrics for early detection
5. Automated rollback scripts

## Next Steps

1. **Create Spring Boot project structure**
2. **Set up database and Redis connections**
3. **Implement basic entities and repositories**
4. **Start with User management module**
5. **Gradually implement other modules**
6. **Continuous testing and validation**

---

*이 문서는 마이그레이션 프로세스를 안내하기 위한 초기 계획입니다. 실제 구현 과정에서 필요에 따라 수정하고 보완될 수 있습니다.*