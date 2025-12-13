# API 아키텍처

n8n 백엔드는 Express.js 기반의 REST API 서버로, 의존성 주입, 모듈화된 컨트롤러, 미들웨어 체인 등 현대적인 Node.js 아키텍처 패턴을 따릅니다.

## 서버 초기화

### Server 클래스 구조

```typescript
// packages/cli/src/Server.ts
@Service()
export class Server extends AbstractServer {
  constructor(
    private readonly loadNodesAndCredentials: LoadNodesAndCredentials,
    private readonly postHogClient: PostHogClient,
    private readonly eventService: EventService,
    private readonly instanceSettings: InstanceSettings,
  ) {
    super();
  }

  async start() {
    // Express 앱 설정
    this.app = express();

    // 미들웨어 등록
    this.registerMiddlewares();

    // 라우트 등록
    this.registerRoutes();

    // 에러 핸들러 등록
    this.registerErrorHandlers();

    // 서버 시작
    this.app.listen(port, this.startupCallback);
  }
}
```

### 의존성 주입 컨테이너

n8n은 `@n8n/di` 패키지를 사용하여 IoC(Inversion of Control) 컨테이너를 구현합니다.

```typescript
// 서비스 등록
Container.set(AuthService, new AuthService());
Container.set(UserService, new UserService());

// 컨트롤러에서 주입받기
@Controller()
export class UsersController {
  constructor(
    private readonly userService: UserService,
    private readonly authService: AuthService,
  ) {}
}
```

## 미들웨어 아키텍처

### 미들웨어 등록 순서

```typescript
private registerMiddlewares() {
  // 1. 보안 미들웨어
  this.app.use(helmet());

  // 2. 요청 파싱
  this.app.use(express.json({ limit: '10mb' }));
  this.app.use(express.urlencoded({ extended: true }));

  // 3. 쿠키
  this.app.use(cookieParser());

  // 4. CORS
  this.app.use(cors(this.corsOptions));

  // 5. 요청 로깅
  this.app.use(this.requestLogger);

  // 6. 인증 미들웨어
  this.app.use(this.authMiddleware);

  // 7. RBAC 미들웨어
  this.app.use(this.rbacMiddleware);
}
```

### 인증 미들웨어

```typescript
// auth.middleware.ts
export const authMiddleware = async (
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const token = extractToken(req);

    if (!token) {
      return next(new UnauthorizedError('No token provided'));
    }

    const user = await jwt.verify(token, process.env.JWT_SECRET);
    req.user = user;

    next();
  } catch (error) {
    next(new UnauthorizedError('Invalid token'));
  }
};
```

### RBAC 미들웨어

```typescript
// rbac.middleware.ts
export const rbacMiddleware = (requiredPermissions: string[]) => {
  return async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    const user = req.user;
    const hasPermission = await checkPermissions(user, requiredPermissions);

    if (!hasPermission) {
      return next(new ForbiddenError('Insufficient permissions'));
    }

    next();
  };
};
```

## 컨트롤러 아키텍처

### 컨트롤러 데코레이터

```typescript
// workflows.controller.ts
@Controller('/workflows')
export class WorkflowsController {
  @Get('/')
  @License('feat:workflows')
  @ACL(['workflow:read'])
  async getWorkflows(req: Request, res: Response): Promise<Response> {
    const workflows = await this.workflowService.getAll(req.user);
    return res.json(workflows);
  }

  @Post('/')
  @ACL(['workflow:create'])
  async createWorkflow(
    @Body() createDto: CreateWorkflowDto,
    req: AuthenticatedRequest,
  ): Promise<Response> {
    const workflow = await this.workflowService.create(createDto, req.user);
    return res.status(201).json(workflow);
  }

  @Get('/:id')
  @Param('id', ParseUUID)
  @ACL(['workflow:read'])
  async getWorkflow(
    @Params('id') id: string,
    req: AuthenticatedRequest,
  ): Promise<Response> {
    const workflow = await this.workflowService.getById(id, req.user);
    return res.json(workflow);
  }
}
```

### DTO(Data Transfer Object)

```typescript
// create-workflow.dto.ts
export class CreateWorkflowDto {
  @IsString()
  @IsNotEmpty()
  name: string;

  @IsOptional()
  @IsArray()
  tags?: string[];

  @IsOptional()
  @IsObject()
  settings?: IWorkflowSettings;

  @ValidateNested()
  @Type(() => NodeDto)
  nodes: NodeDto[];

  @ValidateNested()
  @Type(() => ConnectionDto)
  connections: ConnectionDto;
}
```

### 유효성 검사

```typescript
// validationPipe.ts
export const validationPipe = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const errors = validate(req.body);

  if (errors.length > 0) {
    throw new BadRequestException(errors);
  }

  next();
};
```

## 서비스 레이어

### 서비스 구조

```typescript
// workflow.service.ts
@Injectable()
export class WorkflowService {
  constructor(
    @InjectRepository(Workflow)
    private readonly workflowRepository: Repository<Workflow>,
    private readonly nodeTypeService: NodeTypeService,
    private readonly eventBus: EventBus,
  ) {}

  async create(
    createDto: CreateWorkflowDto,
    user: User,
  ): Promise<Workflow> {
    const workflow = this.workflowRepository.create({
      ...createDto,
      owner: user,
      version: 1,
    });

    const saved = await this.workflowRepository.save(workflow);

    // 이벤트 발행
    this.eventBus.emit('workflow.created', {
      workflowId: saved.id,
      userId: user.id,
    });

    return saved;
  }

  async update(
    id: string,
    updateDto: UpdateWorkflowDto,
    user: User,
  ): Promise<Workflow> {
    const workflow = await this.getById(id, user);

    Object.assign(workflow, updateDto);
    workflow.updatedAt = new Date();

    return this.workflowRepository.save(workflow);
  }
}
```

### 트랜잭션 관리

```typescript
// transaction.service.ts
export class TransactionService {
  async runInTransaction<T>(
    operation: (manager: EntityManager) => Promise<T>
  ): Promise<T> {
    return this.dataSource.transaction(async (manager) => {
      return operation(manager);
    });
  }
}

// 사용 예시
await this.transactionService.runInTransaction(async (manager) => {
  await manager.save(workflow);
  await manager.increment(user, 'workflowCount', 1);
});
```

## 데이터베이스 레이어

### Entity 정의

```typescript
// workflow.entity.ts
@Entity('workflow')
export class Workflow {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  name: string;

  @Column('jsonb')
  nodes: INode[];

  @Column('jsonb')
  connections: IConnections;

  @Column({ default: false })
  active: boolean;

  @ManyToOne(() => User, (user) => user.workflows)
  @JoinColumn({ name: 'ownerId' })
  owner: User;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => Execution, (execution) => execution.workflow)
  executions: Execution[];
}
```

### Repository 패턴

```typescript
// workflow.repository.ts
@Injectable()
export class WorkflowRepository {
  constructor(
    @InjectRepository(Workflow)
    private readonly repository: Repository<Workflow>,
  ) {}

  async findByOwner(ownerId: string): Promise<Workflow[]> {
    return this.repository.find({
      where: { owner: { id: ownerId } },
      relations: ['tags', 'executions'],
      order: { updatedAt: 'DESC' },
    });
  }

  async findActiveWorkflows(): Promise<Workflow[]> {
    return this.repository.find({
      where: { active: true },
      select: ['id', 'nodes', 'connections'],
    });
  }

  async search(query: string, userId: string): Promise<Workflow[]> {
    return this.repository
      .createQueryBuilder('workflow')
      .where('workflow.name ILIKE :query', { query: `%${query}%` })
      .andWhere('workflow.ownerId = :userId', { userId })
      .leftJoinAndSelect('workflow.tags', 'tag')
      .getMany();
  }
}
```

## 에러 핸들링

### 글로벌 에러 핸들러

```typescript
// error-handler.ts
export const globalErrorHandler = (
  error: Error,
  req: Request,
  res: Response,
  next: NextFunction
) => {
  if (error instanceof HttpException) {
    return res.status(error.getStatus()).json({
      error: error.message,
      details: error.details,
    });
  }

  // 로깅
  logger.error('Unhandled error', {
    error: error.message,
    stack: error.stack,
    url: req.url,
    method: req.method,
  });

  // Sentry에 보고
  Sentry.captureException(error);

  res.status(500).json({
    error: 'Internal server error',
  });
};
```

### 커스텀 에러 클래스

```typescript
// errors.ts
export class UnauthorizedError extends HttpException {
  constructor(message: string) {
    super(message, HttpStatus.UNAUTHORIZED);
  }
}

export class ForbiddenError extends HttpException {
  constructor(message: string) {
    super(message, HttpStatus.FORBIDDEN);
  }
}

export class WorkflowNotFoundError extends HttpException {
  constructor(workflowId: string) {
    super(`Workflow with ID ${workflowId} not found`, HttpStatus.NOT_FOUND);
  }
}
```

## API 버저닝

### 버전 관리 전략

```typescript
// v1/workflows.controller.ts
@Controller('/api/v1/workflows')
export class WorkflowsV1Controller {
  // V1 API 구현
}

// v2/workflows.controller.ts
@Controller('/api/v2/workflows')
export class WorkflowsV2Controller {
  // V2 API 구현 (향상된 기능)
}
```

### 버전 협상

```typescript
// version.middleware.ts
export const apiVersionMiddleware = (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const version = req.headers['api-version'] || 'v1';
  req.apiVersion = version;
  next();
};
```

## API 문서화

### Swagger/OpenAPI 통합

```typescript
// swagger.config.ts
const config = new DocumentBuilder()
  .setTitle('n8n API')
  .setVersion('2.0')
  .setDescription('n8n workflow automation API')
  .addBearerAuth()
  .build();

export const document = SwaggerModule.createDocument(app, config);

// 컨트롤러 데코레이터
@ApiOperation({ summary: 'Create a new workflow' })
@ApiResponse({ status: 201, description: 'Workflow created' })
@ApiResponse({ status: 400, description: 'Invalid data' })
@Post('/')
async createWorkflow(@Body() createDto: CreateWorkflowDto) {
  // 구현
}
```

## 웹소켓 API

### WebSocket 서버

```typescript
// websocket.server.ts
export class WebSocketServer {
  private io: Server;

  constructor(private readonly eventBus: EventBus) {
    this.io = new Server(httpServer, {
      cors: { origin: process.env.FRONTEND_URL }
    });

    this.setupEventHandlers();
  }

  private setupEventHandlers() {
    // 워크플로우 실행 상태 업데이트
    this.eventBus.on('execution.started', (data) => {
      this.io.to(`workflow:${data.workflowId}`).emit('execution:started', data);
    });

    this.eventBus.on('execution.completed', (data) => {
      this.io.to(`workflow:${data.workflowId}`).emit('execution:completed', data);
    });
  }

  handleConnection(socket: Socket) {
    // 워크플로우 방에 참여
    socket.on('join-workflow', (workflowId: string) => {
      socket.join(`workflow:${workflowId}`);
    });

    // 실시간 로그 구독
    socket.on('subscribe-logs', (executionId: string) => {
      socket.join(`execution:${executionId}`);
    });
  }
}
```

## API 테스트

### 통합 테스트

```typescript
// workflows.controller.e2e.test.ts
describe('Workflows API', () => {
  let app: INestApplication;
  let authToken: string;

  beforeAll(async () => {
    app = await createTestApp();
    authToken = await getAuthToken(app);
  });

  describe('POST /api/v1/workflows', () => {
    it('should create a new workflow', async () => {
      const createDto = {
        name: 'Test Workflow',
        nodes: [],
        connections: {},
      };

      const response = await request(app.getHttpServer())
        .post('/api/v1/workflows')
        .set('Authorization', `Bearer ${authToken}`)
        .send(createDto)
        .expect(201);

      expect(response.body).toMatchObject({
        name: createDto.name,
        active: false,
      });
    });

    it('should return 400 for invalid data', async () => {
      const response = await request(app.getHttpServer())
        .post('/api/v1/workflows')
        .set('Authorization', `Bearer ${authToken}`)
        .send({})
        .expect(400);

      expect(response.body.error).toContain('name should not be empty');
    });
  });
});
```

## 성능 최적화

### 1. 쿼리 최적화

```typescript
// N+1 문제 해결
const workflows = await this.workflowRepository.find({
  relations: ['owner', 'tags', 'executions'],
  select: {
    id: true,
    name: true,
    active: true,
    owner: { id: true, firstName: true, lastName: true },
    tags: { id: true, name: true },
  },
});
```

### 2. 캐싱

```typescript
// redis.service.ts
@Injectable()
export class CacheService {
  constructor(
    @Inject('REDIS') private readonly redis: Redis,
  ) {}

  async get<T>(key: string): Promise<T | null> {
    const value = await this.redis.get(key);
    return value ? JSON.parse(value) : null;
  }

  async set(key: string, value: any, ttl: number = 300): Promise<void> {
    await this.redis.setex(key, ttl, JSON.stringify(value));
  }
}

// 서비스에서 사용
async getWorkflowTypes(): Promise<NodeType[]> {
  const cacheKey = 'workflow:types';
  let types = await this.cache.get<NodeType[]>(cacheKey);

  if (!types) {
    types = await this.nodeTypeService.getAll();
    await this.cache.set(cacheKey, types, 3600);
  }

  return types;
}
```

### 3. Rate Limiting

```typescript
// rate-limiter.middleware.ts
import rateLimit from 'express-rate-limit';

export const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15분
  max: 1000, // 최대 1000 요청
  message: 'Too many requests',
  standardHeaders: true,
  legacyHeaders: false,
});

// 엔드포인트별 제한
export const webhookLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1분
  max: 100,
  skipSuccessfulRequests: true,
});
```

## 로깅 및 모니터링

### 구조화된 로깅

```typescript
// logger.ts
export class Logger {
  private logger = winston.createLogger({
    format: winston.format.combine(
      winston.format.timestamp(),
      winston.format.errors({ stack: true }),
      winston.format.json()
    ),
    transports: [
      new winston.transports.Console(),
      new winston.transports.File({ filename: 'error.log', level: 'error' }),
    ],
  });

  logRequest(req: Request, res: Response, duration: number) {
    this.logger.info('HTTP Request', {
      method: req.method,
      url: req.url,
      statusCode: res.statusCode,
      duration,
      userAgent: req.headers['user-agent'],
      ip: req.ip,
      userId: req.user?.id,
    });
  }
}
```

### 헬스 체크

```typescript
// health.controller.ts
@Controller('/health')
export class HealthController {
  constructor(
    private readonly databaseService: DatabaseService,
    private readonly redisService: RedisService,
  ) {}

  @Get('/')
  async healthCheck(): Promise<HealthResponse> {
    const checks = await Promise.allSettled([
      this.databaseService.ping(),
      this.redisService.ping(),
    ]);

    return {
      status: checks.every(check => check.status === 'fulfilled') ? 'healthy' : 'degraded',
      timestamp: new Date().toISOString(),
      services: {
        database: checks[0].status === 'fulfilled' ? 'up' : 'down',
        redis: checks[1].status === 'fulfilled' ? 'up' : 'down',
      },
    };
  }
}
```

## 보안 구현

### 1. 입력 검증

```typescript
// sanitizer.middleware.ts
export const inputSanitizer = (req: Request, res: Response, next: NextFunction) => {
  if (req.body) {
    req.body = DOMPurify.sanitize(req.body);
  }
  next();
};
```

### 2. SQL Injection 방지

```typescript
// 파라미터화된 쿼리 사용
const user = await this.userRepository
  .createQueryBuilder('user')
  .where('user.email = :email', { email })
  .getOne();
```

### 3. XSS 방지

```typescript
// response-sanitizer.ts
export const sanitizeResponse = (data: any): any => {
  if (typeof data === 'string') {
    return DOMPurify.sanitize(data);
  }

  if (Array.isArray(data)) {
    return data.map(sanitizeResponse);
  }

  if (typeof data === 'object' && data !== null) {
    const sanitized: any = {};
    for (const [key, value] of Object.entries(data)) {
      sanitized[key] = sanitizeResponse(value);
    }
    return sanitized;
  }

  return data;
};
```

## 모범 사례

1. **컨트롤러**: HTTP 요청/응답만 처리, 비즈니스 로직은 서비스로 분리
2. **서비스**: 비즈니스 로직 구현, 여러 리포지토리 조합
3. **리포지토리**: 데이터베이스 접근 로직만 담당
4. **DTO**: 요청/응답 데이터 구조 정의 및 유효성 검사
5. **에러 처리**: 구체적인 에러 타입 사용, 적절한 HTTP 상태 코드 반환
6. **테스트**: 각 레이어별 단위 테스트, API 통합 테스트
7. **문서화**: OpenAPI/Swagger로 API 자동 문서화
8. **보안**: 입력 검증, 인증/권한 체크, 민감정보 암호화