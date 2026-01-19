# Spring Framework Integration Guide

## 📋 Overview

- This guide demonstrates how to integrate the MaskMe library with Spring Framework applications.
- Leveraging Spring's dependency injection, configuration management, and web capabilities.

## 🚀 Quick Setup

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
