# Spring Framework Integration Guide

## 📋 Overview

- This guide demonstrates how to integrate the MaskMe library with Spring Framework applications.
- Leveraging Spring's dependency injection, configuration management, and web capabilities.

## 🚀 Running the Application

### Prerequisites

- Java 21+
- Maven 3.6+

### Start the Application

```bash
# Clone and navigate to the project
cd Spring-maskme

# Build and run
mvn clean install
mvn spring-boot:run
```

The application starts on `http://localhost:9090`

### Available Endpoints

| Endpoint             | Method | Headers                                       | Description                       |
|----------------------|--------|-----------------------------------------------|-----------------------------------|
| `/users/{id}`        | GET    | -                                             | Get user without masking          |
| `/users/masked/{id}` | GET    | `Mask-Input: maskMe`                          | Get user with conditional masking |
| `/users`             | GET    | `Mask-Input: maskMe`<br>`Mask-Phone: {phone}` | Get all users with masking        |
| `/users/user/{id}`   | GET    | -                                             | Get domain entity with masking    |

### Test with cURL

```bash
# 1. Get user without masking
curl http://localhost:9090/users/1

# 2. Get user with masking enabled
curl -H "Mask-Input: maskMe" http://localhost:9090/users/masked/1

# 3. Get all users with phone masking for specific number
curl -H "Mask-Input: maskMe" -H "Mask-Phone: 01000000000" http://localhost:9090/users

# 4. Get domain entity (always masks certain fields)
curl http://localhost:9090/users/user/1
```

### Expected Responses

#### GET `/users/1` (No Masking)

```json
{
  "id": 1,
  "name": "Ahmed Samy",
  "email": "one@mail.com",
  "password": "123456",
  "phone": "01000000000",
  "address": {
    "id": 1,
    "street": "first Street",
    "city": "City One",
    "zipCode": "Zip One"
  },
  "birthDate": "1985-01-25",
  "balance": 20.0
}
```

#### GET `/users/masked/1` with `Mask-Input: maskMe` (With Masking)

```json
{
  "id": 1000,
  "name": "one@mail.com-M",
  "email": "[EMAIL PROTECTED]",
  "password": "************",
  "phone": "01000000000",
  "address": {
    "id": 1,
    "street": "first Street",
    "city": "***",
    "zipCode": "[ZIP_MASKED]"
  },
  "birthDate": "1800-01-01",
  "balance": 0
}
```

#### GET `/users` with `Mask-Phone: 01000000000` (Selective Phone Masking)

```json
[
  {
    "id": 1000,
    "phone": "***",
    "...": "other fields masked"
  },
  {
    "id": 1000,
    "phone": "01000000011",
    "...": "other fields masked but phone not masked"
  }
]
```

## 🔧 Configuration Setup

### Step 1: Framework Configuration

Configure MaskMe with Spring's ApplicationContext for dependency injection support:

```java
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MaskingConfiguration {

    private final ApplicationContext applicationContext;

    @PostConstruct
    public void setupMaskMe() {
        // Register framework provider for dependency injection
        registerFrameworkProvider();
        
        // Configure a custom field regex pattern (optional)
        configureFieldPattern();
        
        // Clear and register custom converters
        setupCustomConverters();
    }

    private void registerFrameworkProvider() {
        MaskMeConditionFactory.setFrameworkProvider(new MaskMeFrameworkProvider() {
            @Override
            public <T> T getInstance(Class<T> type) {
                try {
                    return applicationContext.getBean(type);
                } catch (Exception e) {
                    log.warn("Failed to get bean of type {} from Spring context", type.getName(), e);
                    throw new MaskMeException("Failed to get bean: " + type.getName(), e);
                }
            }
        });
    }

    private void configureFieldPattern() {
        // Optional: Configure a custom field reference pattern 
        // Default is {fieldName}, you can change to [fieldName] or others
        MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\{([^}]+)\\}"));
    }

    private void setupCustomConverters() {
        MaskMeConverterRegistry.clearGlobal();
        MaskMeConverterRegistry.registerGlobal(new CustomEmailConverter());
        MaskMeConverterRegistry.registerGlobal(new CustomPhoneConverter());
    }

    // Declare built-in conditions as beans to avoid NoSuchBeanDefinitionException, because the library is pure java.
    @Bean
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }

    @Bean
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }

    @PreDestroy
    public void cleanup() {
        MaskMeConverterRegistry.clearGlobal();
    }
}
```

### Step 2: Custom Conditions with Spring DI

Create Spring-managed conditions with dependency injection:

```java
@Component
public class RoleBasedMaskCondition implements MaskMeCondition {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SecurityService securityService;
    
    private String requiredRole;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.requiredRole = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        String currentUserRole = securityService.getCurrentUserRole();
        return !requiredRole.equals(currentUserRole);
    }
}

@Component
public class EnvironmentBasedCondition implements MaskMeCondition {
    
    @Value("${app.masking.enabled:true}")
    private boolean maskingEnabled;
    
    @Autowired
    private Environment environment;
    
    private String environmentFlag;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.environmentFlag = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        if (!maskingEnabled) return false;
        
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).contains(environmentFlag);
    }
}
```

### Step 3: Default Conditions Usage

Use built-in conditions in your DTOs:

```java
public record UserDto(
    @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "****")
    String password,
    
    @MaskMe(conditions = {MaskMeOnInput.class}, maskValue = "{firstName}@masked.com")
    String email,
    
    @MaskMe(conditions = {RoleBasedMaskCondition.class}, maskValue = "***-**-****")
    String ssn,
    
    String firstName,
    String lastName
) {}
```

### Step 4: Field Reference Configuration

Configure custom field reference patterns at startup:

```java
@Configuration
public class FieldPatternConfiguration {
    
    @PostConstruct
    public void configureFieldPattern() {
        // Use square brackets instead of curly braces
        MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\[([^]]+)]"));
    }
}

// Now use in DTOs:
public record ProductDto(
    @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "[name]-MASKED")
    String description,
    
    String name
) {}
```

### Step 5: Custom Converters

Implement Spring-aware custom converters:

```java
@Component
public class SpringEmailConverter implements MaskMeConverter {
    
    @Autowired
    private MaskingProperties maskingProperties;
    
    @Override
    public int getPriority() {
        return 10; // Higher than defaults
    }
    
    @Override
    public boolean canConvert(Class<?> type) {
        return String.class.equals(type);
    }
    
    @Override
    public Object convert(String maskValue, Class<?> targetType, Object originalValue, 
                         Object objectContainingMaskedField, String maskedFieldName) {
        
        String processedValue = MaskMeFieldAccessUtil
            .getMaskedValueFromAnotherFieldOrMaskedValue(maskValue, objectContainingMaskedField);
        
        if (fieldName.toLowerCase().contains("email")) {
            return maskingProperties.getEmailMaskPattern().replace("{value}", processedValue);
        }
        
        return processedValue;
    }
}

@ConfigurationProperties(prefix = "app.masking")
@Data
public class MaskingProperties {
    private String emailMaskPattern = "{value}@masked.com";
    private boolean enabled = true;
}
```

## 📚 Practical Examples

### Example 1: @ExcludeMaskMe Annotation

Use `@ExcludeMaskMe` to prevent masking of entire objects or fields:

```java
public record UserDto(
    String name,
    String email,
    
    // This entire address object will NOT be processed for masking
    @ExcludeMaskMe
    AddressDto address,
    
    @MaskMe(conditions = {AlwaysMaskMeCondition.class})
    String ssn
) {}

public record AddressDto(
    String street,
    @MaskMe(conditions = {AlwaysMaskMeCondition.class})
    String city  // This will NOT be masked because parent has @ExcludeMaskMe
) {}
```

**Use Cases:**

- Exclude complex nested objects from masking processing
- Improve performance by skipping unnecessary fields
- Preserve original data for non-sensitive fields

### Example 2: Multiple Conditions on Same Field

```java
public record SensitiveDto(
    // Masked if ANY condition returns true (OR logic)
    @MaskMe(
        conditions = {RoleBasedCondition.class, EnvironmentCondition.class, TimeBasedCondition.class},
        maskValue = "***"
    )
    String sensitiveData
) {}

// Usage in controller
@GetMapping("/sensitive/{id}")
public SensitiveDto getData(@PathVariable Long id) {
    SensitiveDto dto = service.getData(id);
    
    return MaskMeInitializer.mask(dto,
        RoleBasedCondition.class, "ADMIN",
        EnvironmentCondition.class, "production",
        TimeBasedCondition.class, LocalTime.now()
    );
}
```

**Behavior:** Field is masked if **ANY** condition evaluates to `true`.

### Example 3: Error Handling in Controllers

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(
            @PathVariable Long id,
            @RequestHeader(value = "Mask-Input", required = false) String maskInput) {
        
        try {
            User user = userService.findUserById(id);
            UserDto dto = userMapper.toDto(user);
            
            // Apply masking if header is present
            if (maskInput != null && !maskInput.isBlank()) {
                dto = MaskMeInitializer.mask(dto, MaskMeOnInput.class, maskInput);
            }
            
            return ResponseEntity.ok(dto);
            
        } catch (MaskMeException e) {
            log.error("Masking failed for user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/batch")
    public ResponseEntity<List<UserDto>> getBatchUsers(
            @RequestParam List<Long> ids,
            @RequestHeader(value = "Mask-Input", required = false) String maskInput) {
        
        try {
            List<UserDto> users = ids.stream()
                .map(userService::findUserById)
                .map(userMapper::toDto)
                .map(dto -> {
                    try {
                        return maskInput != null 
                            ? MaskMeInitializer.mask(dto, MaskMeOnInput.class, maskInput)
                            : dto;
                    } catch (MaskMeException e) {
                        log.warn("Failed to mask user, returning unmasked: {}", e.getMessage());
                        return dto; // Fallback to unmasked
                    }
                })
                .toList();
            
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            log.error("Batch processing failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @ExceptionHandler(MaskMeException.class)
    public ResponseEntity<ErrorResponse> handleMaskingException(MaskMeException e) {
        log.error("Masking error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("MASKING_ERROR", "Failed to mask sensitive data"));
    }
}

record ErrorResponse(String code, String message) {}
```

### Example 4: Logging Output

#### Enable Logging in Configuration

```java
@Configuration
public class MaskMeConfiguration {
    
    @PostConstruct
    public void setupMaskMe() {
        // Enable DEBUG level logging
        MaskMeLogger.enable(Level.FINE);
        
        log.info("MaskMe library initialized with logging enabled");
    }
}
```

#### Expected Log Output

```log
2025-01-10T10:15:30.123+02:00  INFO 12345 --- [main] c.j.m.m.c.MaskMeConfiguration : MaskMe library initialized with logging enabled

2025-01-10T10:15:31.456+02:00  INFO 12345 --- [main] c.j.m.a.c.MaskMeConditionFactory : [DEBUGGING] Registering framework provider: SpringFrameworkProvider

2025-01-10T10:15:32.789+02:00  INFO 12345 --- [nio-9090-exec-1] c.j.m.i.c.MaskMeOnInput : [DEBUGGING] Input set to: maskMe

2025-01-10T10:15:32.890+02:00  INFO 12345 --- [nio-9090-exec-1] c.j.m.i.c.AlwaysMaskMeCondition : [DEBUGGING] Condition evaluated: shouldMask=true for field=password

2025-01-10T10:15:32.991+02:00  INFO 12345 --- [nio-9090-exec-1] c.j.m.a.c.MaskMeConverter : [DEBUGGING] Converting field 'email' from String to String with maskValue='[EMAIL PROTECTED]'

2025-01-10T10:15:33.092+02:00  WARN 12345 --- [nio-9090-exec-1] c.j.m.a.c.MaskMeConverterRegistry : Fallback conversion used for field 'balance' - no custom converter found

2025-01-10T10:15:33.193+02:00  INFO 12345 --- [nio-9090-exec-1] c.j.m.MaskMeProcessor : Successfully masked 5 fields in UserDto
```

#### Logging Levels Explained

- **INFO**: High-level operations (initialization, successful masking)
- **DEBUG** (`Level.FINE`): Detailed traces (condition evaluation, field processing)
- **WARN**: Recoverable issues (fallback conversions, missing fields)
- **ERROR**: Critical failures (condition creation errors, invalid inputs)

#### Disable Logging for Production

```java
@Configuration
@Profile("production")
public class ProductionMaskMeConfiguration {
    
    @PostConstruct
    public void setupMaskMe() {
        // Disable logging for zero overhead
        MaskMeLogger.disable();
    }
}
```

## 🎯 Controller Integration

### Using MaskMeInitializer (Recommended)

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id,
                          @RequestHeader(value = "X-Mask-Level", defaultValue = "none") String maskLevel) {
        
        User user = userService.findById(id);
        UserDto dto = userMapper.toDto(user);
        
        return MaskMeInitializer.mask(dto,
            MaskMeOnInput.class, maskLevel,
            RoleBasedMaskCondition.class, "ADMIN"
        );
    }
    
    @GetMapping
    public List<UserDto> getUsers(@RequestHeader(value = "X-Environment", defaultValue = "prod") String env) {
        
        return userService.findAll().stream()
            .map(userMapper::toDto)
            .map(dto -> MaskMeInitializer.mask(dto, EnvironmentBasedCondition.class, env))
            .toList();
    }
}
```

### Using MaskMeProcessor with Spring DI

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final MaskMeProcessor maskProcessor;
    
    @GetMapping("/{id}/detailed")
    public UserDto getDetailedUser(@PathVariable Long id,
                                  @RequestParam(defaultValue = "false") boolean maskSensitive) {
        
        try {
            maskProcessor.setConditionInput(MaskMeOnInput.class, maskSensitive ? "maskMe" : "none");
            
            User user = userService.findById(id);
            UserDto dto = userMapper.toDto(user);
            
            return maskProcessor.process(dto);
        } finally {
            maskProcessor.clearInputs();
        }
    }
}
```

## 🔧 Advanced Spring Integration

### Request-Scoped Converters

```java
@RestController
@RequiredArgsConstructor
public class AdvancedMaskingController {
    
    @GetMapping("/users/{id}/custom-mask")
    public UserDto getUserWithCustomMask(@PathVariable Long id,
                                        @RequestHeader("X-Mask-Pattern") String pattern) {
        
        // Register request-scoped converter
        MaskMeConverterRegistry.registerRequestScoped(new CustomPatternConverter(pattern));
        
        try {
            User user = userService.findById(id);
            return MaskMeInitializer.mask(userMapper.toDto(user));
        } finally {
            MaskMeConverterRegistry.clearRequestScoped();
        }
    }
}

public class CustomPatternConverter implements MaskMeConverter {
    private final String pattern;
    
    public CustomPatternConverter(String pattern) {
        this.pattern = pattern;
    }
    
    @Override
    public int getPriority() {
        return 15; // Highest priority
    }
    
    @Override
    public boolean canConvert(Class<?> type) {
        return String.class.equals(type);
    }
    
    @Override
    public Object convert(String maskValue, Class<?> targetType, Object originalValue,
                         Object objectContainingMaskedField, String maskedFieldName) {
        return pattern.replace("{original}", String.valueOf(originalValue));
    }
}
```

### Spring Security Integration

```java
@Component
public class SecurityAwareMaskCondition implements MaskMeCondition {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    private String requiredAuthority;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.requiredAuthority = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return true; // Mask for unauthenticated users
        }
        
        return auth.getAuthorities().stream()
            .noneMatch(authority -> authority.getAuthority().equals(requiredAuthority));
    }
}

// Usage in controller
@GetMapping("/secure/{id}")
@PreAuthorize("hasRole('USER')")
public UserDto getSecureUser(@PathVariable Long id) {
    User user = userService.findById(id);
    UserDto dto = userMapper.toDto(user);
    
    return MaskMeInitializer.mask(dto, SecurityAwareMaskCondition.class, "ROLE_ADMIN");
}
```

### Configuration Properties Integration

```java
@ConfigurationProperties(prefix = "maskme")
@Configuration
@Data
public class MaskMeProperties {
    
    private boolean enabled = true;
    private String defaultMaskValue = "***";
    private FieldPattern fieldPattern = FieldPattern.CURLY_BRACES;
    private Map<String, String> customMasks = new HashMap<>();
    
    public enum FieldPattern {
        CURLY_BRACES("\\{([^}]+)\\}"),
        SQUARE_BRACKETS("\\[([^]]+)]"),
        PARENTHESES("\\(([^)]+)\\)");
        
        private final String pattern;
        
        FieldPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public Pattern getCompiledPattern() {
            return Pattern.compile(pattern);
        }
    }
}

@Configuration
@RequiredArgsConstructor
public class MaskMeAutoConfiguration {
    
    private final MaskMeProperties properties;
    
    @PostConstruct
    public void configure() {
        if (properties.isEnabled()) {
            MaskMeFieldAccessUtil.setUserPattern(properties.getFieldPattern().getCompiledPattern());
        }
    }
}
```

## 🧪 Testing with Spring

### Running the Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserControllerIntegrationTest

# Run with verbose output
mvn test -X
```

### Test Coverage

The `UserControllerIntegrationTest` covers all key MaskMe features:

| Test                                                             | What It Verifies                                         |
|------------------------------------------------------------------|----------------------------------------------------------|
| `getUserById_WithoutMasking_ReturnsOriginalData`                 | Unmasked endpoint returns original data                  |
| `getMaskedUserById_WithMaskInput_MasksSensitiveFields`           | MaskMeOnInput condition masks fields correctly           |
| `getMaskedUserById_WithDifferentInput_DoesNotMask`               | Conditions only trigger with matching input              |
| `getUsers_WithPhoneMasking_MasksOnlyMatchingPhone`               | Custom PhoneMaskingCondition works with DI               |
| `getUser_DomainEntity_MasksAlwaysConditionFields`                | AlwaysMaskMeCondition masks without input                |
| `getMaskedUserById_WithExcludeMaskMe_DoesNotProcessNestedFields` | @ExcludeMaskMe prevents nested processing                |
| `getMaskedUserById_WithFieldReference_ReplacesPlaceholder`       | Field referencing {fieldName} works                      |
| `getMaskedUserById_WithCustomConverter_UsesCustomLogic`          | Custom converters override defaults                      |
| `getMaskedUserById_WithDifferentTypes_ConvertsCorrectly`         | Type conversion for Long, LocalDate, BigDecimal, Instant |
| `getMaskedUserById_WithNestedObject_MasksNestedFields`           | Nested object masking works recursively                  |

### Expected Test Output

```bash
$ mvn test -Dtest=UserControllerIntegrationTest

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.javamsdt.masking.controller.UserControllerIntegrationTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

### Writing Your Own Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MyMaskingTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testCustomMasking() throws Exception {
        MvcResult result = mockMvc.perform(get("/your-endpoint")
                        .header("Your-Header", "value"))
                .andExpect(status().isOk())
                .andReturn();
        
        YourDto dto = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            YourDto.class
        );
        
        assertThat(dto.maskedField()).isEqualTo("expected-masked-value");
    }
}
```

### Unit Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "maskme.enabled=true",
    "maskme.default-mask-value=TEST_MASK"
})
class MaskingIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testMaskingWithSpringContext() {
        // Given
        User user = createTestUser();
        UserDto dto = userMapper.toDto(user);
        
        // When
        UserDto masked = MaskMeInitializer.mask(dto, MaskMeOnInput.class, "maskMe");
        
        // Then
        assertThat(masked.password()).isEqualTo("****");
        assertThat(masked.email()).contains("@masked.com");
    }
    
    @Test
    void testCustomConditionWithDI() {
        // Given
        UserDto dto = createTestDto();
        
        // When
        UserDto masked = MaskMeInitializer.mask(dto, RoleBasedMaskCondition.class, "USER");
        
        // Then
        assertThat(masked.ssn()).isEqualTo("***-**-****");
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testMaskedEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("X-Mask-Level", "maskMe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("****"))
                .andExpect(jsonPath("$.email").value(containsString("@masked.com")));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testSecureEndpointWithAdmin() throws Exception {
        mockMvc.perform(get("/secure/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssn").value(not("***-**-****"))); // Not masked for admin
    }
}
```

## 📝 Best Practices

### 1. Startup Configuration

- Configure a framework provider once at application startup
- Clear global converters to prevent memory leaks
- Register custom converters after clearing globals

### 2. Bean Management

- Declare built-in conditions as Spring beans
- Use `@Component` for custom conditions requiring DI
- Leverage `@ConfigurationProperties` for configuration

### 3. Controller Design

- Use MaskMeInitializer for cleaner code
- Handle request headers for dynamic masking
- Implement proper error handling

### 4. Memory Management

- Use `@PreDestroy` to clear global converters
- Clear request-scoped converters in finally blocks
- Avoid memory leaks with proper cleanup

### 5. Security Integration

- Integrate with Spring Security for role-based masking
- Use method-level security annotations
- Implement authentication-aware conditions

## ⚠️ Common Issues & Solutions

### Issue 1: NoSuchBeanDefinitionException

```java
// Problem: Built-in conditions not found in Spring context
// Solution: Declare them as beans
@Bean
public AlwaysMaskMeCondition alwaysMaskMeCondition() {
    return new AlwaysMaskMeCondition();
}
```

### Issue 2: Memory Leaks

```java
// Problem: Global converters not cleared
// Solution: Proper cleanup
@PreDestroy
public void cleanup() {
    MaskMeConverterRegistry.clearGlobal();
}
```

### Issue 3: Field Pattern Not Working

```java
// Problem: Pattern not configured at startup
// Solution: Configure in @PostConstruct
@PostConstruct
public void configurePattern() {
    MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\{([^}]+)\\}"));
}
```

---
