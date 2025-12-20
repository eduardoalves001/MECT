---
applyTo: '**'
---
# Risk & Threat Modelling Platform (RTMP)

## Architecture Overview

3-tier containerized application with strict separation of concerns:
- **Frontend**: React + Vite + Shadcn UI + Keycloak auth
- **Backend**: Spring Boot REST API with JWT validation
- **Database**: PostgreSQL with Hibernate auto-DDL
- **Auth**: Keycloak OAuth2/OIDC (role-based access control)
- **Feature Flags**: Flagsmith for feature toggles
- **Reverse Proxy**: Nginx (production only)

**Data Model Hierarchy**: ThreatModel -> Component -> Vulnerability (JPA entities with bidirectional relationships)

## General Guidelines

- Always follow clean code principles
- Keep the code simple and modular
- Keep the code DRY (Don't Repeat Yourself)
- Use meaningful names for variables, functions, and classes
- Only write temporary tests if necessary, delete them later
- Do not add useless comments
- Do not use JavaDoc comments
- Keep documentation in the "docs" folder
- Do not write new documentation files. If necessary, update existing ones
- Do not create ".md" files with explanations
- Try to not leave any hard-coded variables in the source code. If necessary, pass them as env vars
- ALWAYS use Makefile commands - never raw docker compose

## Environment Setup & Commands

Makefile commands:
```bash
make prepare-env    # First-time setup: creates .env from .env-sample
make build          # Build dev containers
make up             # Start dev mode (hot reload enabled)
make upd            # Start dev mode (detached)
make down           # Stop dev containers
make logs           # View dev logs
make test_units     # Run backend tests + generate coverage report
make test_e2e_docker # Run Cypress E2E tests in Docker
```

## Environment Profiles

**Dev**: Uses docker-compose.yaml with profiles: ["dev"], exposes ports directly (8090=frontend, 8095=backend, 8097=keycloak)
**Prod**: Uses profiles: ["prod"], all traffic through Nginx reverse proxy on 443, Keycloak path=/auth
Backend auto-selects application-{dev|prod}.properties via SPRING_PROFILES_ACTIVE env var

## Test Users (Keycloak pre-configured)

| Email | Password | Role |
|-------|----------|------|
| architect@rtmp.com | Architect@2024 | Security Architect |
| developer@rtmp.com | Developer@2024 | Software Developer |

## Frontend

### Technologies
- React
- Shadcn
- Tailwind CSS

### Guidelines
- ONLY use Shadcn components from @/components/ui/ - no custom CSS or styled-components
- ONLY use Tailwind CSS classes - no inline styles or CSS modules
- Import Shadcn via: npx shadcn@latest add <component> (updates components.json)

### Authentication Flow (Keycloak)
- AuthContext wraps app, initializes Keycloak on mount with check-sso + PKCE
- apiClient.ts auto-refreshes tokens (30s threshold), attaches Authorization: Bearer <token> to all requests
- Silent SSO: Uses /silent-check-sso.html iframe for token refresh without redirects

### Routing (Custom Hook)
- Uses RouterContext + useRouter() hook (NOT react-router)
- Navigation: navigateTo("models") or navigateTo("model-detail", { modelId: "uuid" })
- Current page tracked in currentPage state

### API Layer Pattern
All API calls in src/api/:
```typescript
// threatModelApi.ts
import { apiRequest } from './apiClient';
export const getAllThreatModels = () =>
  apiRequest<ApiResponse<ThreatModel[]>>('/api/v1/threat-models');
```
apiClient.ts handles token refresh, error handling, 204 responses


## Backend

### Technologies
- Spring Boot

### Package Structure (STRICT)
```
com.ua.rtmp/
├── config/          # @Configuration classes (Security, CORS, ModelMapper, Flagsmith)
├── constants/       # Static constants (SecurityConstants)
├── dto/response/    # Response DTOs (ApiResponse<T>, stats DTOs)
├── exception/       # Custom exceptions + @RestControllerAdvice global handler
├── mapper/          # ModelMapper beans
├── model/           # @Entity JPA models (ThreatModel, Component, Vulnerability, Threat)
├── repository/      # @Repository Spring Data JPA interfaces
├── resource/        # @RestController classes (NOT "controller" - uses "Resource" suffix)
└── service/         # @Service business logic
```

### Guidelines
- Follow RESTful API design principles
- Validate all inputs and handle errors gracefully

### Security & Authorization Pattern
ALL endpoints require Keycloak JWT authentication except paths in SecurityConstants.PUBLIC_PATHS:
```java
@PreAuthorize("hasRole('threatmodel:read')")  // Role format: resource:action
@GetMapping
public ResponseEntity<ApiResponse<List<ThreatModel>>> getAllThreatModels() { ... }
```
Roles extracted from JWT realm_access.roles claim, prefixed with ROLE_ in Spring Security

### REST Response Pattern
ALWAYS wrap responses in ApiResponse<T>:
```java
return ResponseEntity.ok(ApiResponse.success("Message", data));
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created", created));
```

### Entity Relationships
- Use @OneToMany(mappedBy = "...", cascade = CascadeType.ALL, orphanRemoval = true)
- Parent side: @JsonIgnore on collections to avoid circular serialization
- Child side: @ManyToOne(fetch = FetchType.LAZY) with @JoinColumn
- Always provide helper methods like addComponent(), removeComponent() to maintain bidirectional sync

### Configuration Sources
- Env vars injected via docker-compose.yaml -> read in application-{profile}.properties
- NEVER hardcode secrets/URLs - use ${ENV_VAR} placeholders
- Database schema auto-created by Hibernate (ddl-auto=update in dev)

### Testing Conventions

#### Test Structure
Use nested test classes for better organization:
```java
@DisplayName("ThreatModel Entity Tests")
class ThreatModelTest {
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        @Test
        @DisplayName("Given valid data, when creating with setters, should succeed")
        void givenValidData_whenCreatingWithSetters_shouldSucceed() { ... }
    }
}
```

#### Test Data Builders
Create builder classes in src/test/java/com/ua/rtmp/util/:
```java
public class ThreatModelTestDataBuilder {
    public static ThreatModelTestDataBuilder aThreatModel() { ... }
    public static ThreatModelTestDataBuilder aPaymentSystemThreatModel() { ... }
    public ThreatModelTestDataBuilder withName(String name) { ... }
    public ThreatModel build() { ... }
}
```

#### Testing Layers
1. **Entity Tests**: Validate constructors (no-args, all-args), test validation annotations (@NotBlank, @Size), verify object contracts (equals, hashCode, toString), test bidirectional relationships
2. **Repository Tests** (@DataJpaTest): Save, retrieve, update, delete operations, custom query methods (e.g., existsByName), case sensitivity, unique constraints, empty database scenarios
3. **Service Tests** (@ExtendWith(MockitoExtension.class)): Use @Mock for dependencies, @InjectMocks for service, test business logic (duplicate prevention, validation), transaction handling, exception scenarios
4. **Resource Tests** (@WebMvcTest): ALWAYS exclude security with @WebMvcTest(value = XResource.class, excludeAutoConfiguration = SecurityAutoConfiguration.class), import test config @Import({TestFlagsmithConfig.class, FeatureFlagService.class}), test HTTP status codes (200, 201, 400, 403, 404), verify JSON responses with jsonPath(), test feature flag integration

#### Assertions
Use AssertJ for fluent assertions:
```java
assertThat(violations).isEmpty();
assertThat(result).isPresent();
assertThat(list).hasSize(3);
assertThat(model).isEqualTo(expected).hasSameHashCodeAs(expected);
```

#### Test Organization
- Group related tests in nested classes by operation: ConstructorTests, ValidationTests, ObjectContractTests, SaveAndRetrieveTests, DeleteTests, UpdateTests, GetAllTests, GetByIdTests, CreateTests
- Use descriptive @DisplayName for test context
- Follow naming: givenX_whenY_thenZ pattern

## Database

### Technologies
- PostgreSQL (Hibernate DDL)

## Integration Points

### Keycloak Setup
- Dev realm auto-configured via scripts/setup-keycloak.sh
- Roles format: <resource>:<action> (e.g., threatmodel:read, component:create)
- Backend validates JWT via spring.security.oauth2.resourceserver.jwt.issuer-uri

### Flagsmith Feature Flags
- Backend: FeatureFlagService queries Flagsmith API
- Frontend: Direct API calls using VITE_FLAGSMITH_ENVIRONMENT_KEY
- Setup script: scripts/setup-flagsmith-features.sh

### Database Migrations
- Hibernate manages schema - no Flyway/Liquibase
- Init script: init-db.sh creates separate DBs for app + keycloak + flagsmith
- Access via pgAdmin (dev): http://localhost:8096

## Project Structure

```
├── Docker & Deployment
│   ├── docker-compose.yaml          # Container orchestration
│   ├── Makefile                     # Build & deployment commands
│   └── .env                         # Environment variables
│
├── Backend (Spring Boot)
│   ├── Configuration
│   │   ├── Dockerfile.dev / .prod   # Container configs
│   │   ├── pom.xml                  # Maven dependencies
│   │   ├── application-dev.properties
│   │   └── application-prod.properties
│   │
│   └── Source Code
│       ├── RtmpApplication.java     # Main application
│       ├── config/
│       │   └── CorsConfig.java      # CORS configuration
│       ├── model/
│       │   └── ThreatModel.java     # Entity (auto-creates DB schema)
│       ├── repository/
│       │   └── ThreatModelRepository.java
│       ├── service/
│       │   └── ThreatModelService.java
│       ├── resource/ (Controllers)
│       │   └── ThreatModelResource.java
│       ├── dto/response/
│       │   └── ApiResponse.java
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           ├── DuplicateResourceException.java
│           └── ResourceNotFoundException.java
│
├── Frontend (React + Vite)
│   ├── Configuration
│   │   ├── Dockerfile / .prod       # Container configs
│   │   ├── package.json             # Dependencies
│   │   ├── vite.config.ts          # Build config
│   │   ├── tsconfig.json           # TypeScript config
│   │   └── components.json         # Shadcn UI config
│   │
│   └── Source Code
│       ├── App.tsx                  # Main component
│       ├── main.tsx                # Entry point
│       ├── pages/
│       │   ├── Home/index.tsx
│       │   └── Login/index.tsx
│       ├── components/ui/        # Shadcn components
│       │   ├── button.tsx
│       │   └── table.tsx
│       ├── lib/
│       │   └── utils.ts
│       └── utils/
│           └── toast.tsx
│
└── Documentation
    ├── README.md
    ├── docs/project_specification.md
    └── .github/instructions/project.instructions.md
```