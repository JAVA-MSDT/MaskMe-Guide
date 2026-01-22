# Quarkus Framework Integration Guide

## 📋 Overview

- This guide demonstrates how to integrate the MaskMe library with Quarkus applications.
- Leveraging CDI (Contexts and Dependency Injection), configuration management, and native compilation support.

## 🚀 Quick Setup

### Step 1: Framework Configuration

Configure MaskMe with Quarkus CDI for dependency injection support:

```java
@ApplicationScoped
public class MaskingConfiguration {

    @Inject
    BeanManager beanManager;

    @PostConstruct
    public void setupMaskMe() {
        // Register framework provider for CDI support
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
                    Set<Bean<?>> beans = beanManager.getBeans(type);
                    if (beans.isEmpty()) {
                        return null;
                    }
                    Bean<?> bean = beanManager.resolve(beans);
                    CreationalContext<?> context = beanManager.createCreationalContext(bean);
                    return (T) beanManager.getReference(bean, type, context);
                } catch (Exception e) {
                    Log.warnf("Failed to get CDI bean of type %s", type.getName());
                    throw new MaskMeException("Failed to get CDI bean: " + type.getName(), e);
                }
            }
        });
    }

    // Declare built-in conditions as beans to avoid NoSuchBeanDefinitionException, because the library is pure java.
    @Produces
    @ApplicationScoped
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }

    @Produces
    @ApplicationScoped
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }
    
    private void configureFieldPattern() {
        // Optional: Configure a custom field reference pattern
        MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\{([^}]+)\\}"));
    }

    private void setupCustomConverters() {
        MaskMeConverterRegistry.clearGlobal();
        MaskMeConverterRegistry.registerGlobal(new QuarkusEmailConverter());
        MaskMeConverterRegistry.registerGlobal(new QuarkusPhoneConverter());
    }

    @PreDestroy
    public void cleanup() {
        MaskMeConverterRegistry.clearGlobal();
    }
}
```

### Step 2: Custom Conditions with CDI

Create CDI-managed conditions with dependency injection:

```java
@ApplicationScoped
public class RoleBasedMaskCondition implements MaskMeCondition {
    
    @Inject
    UserService userService;
    
    @Inject
    SecurityIdentity securityIdentity;
    
    private String requiredRole;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.requiredRole = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return !securityIdentity.hasRole(requiredRole);
    }
}

@ApplicationScoped
public class ConfigBasedCondition implements MaskMeCondition {
    
    @ConfigProperty(name = "app.masking.enabled", defaultValue = "true")
    boolean maskingEnabled;
    
    @ConfigProperty(name = "app.environment", defaultValue = "prod")
    String environment;
    
    private String targetEnvironment;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.targetEnvironment = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return maskingEnabled && environment.equals(targetEnvironment);
    }
}
```

### Step 3: Built-in Conditions as CDI Beans

Declare built-in conditions as CDI beans:

```java
@ApplicationScoped
public class MaskingConditionProducer {
    
    @Produces
    @ApplicationScoped
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }
    
    @Produces
    @ApplicationScoped
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }
}
```

### Step 4: Configuration Properties

Use Quarkus configuration for MaskMe settings:

```java
@ConfigMapping(prefix = "maskme")
public interface MaskMeConfig {
    
    @WithDefault("true")
    boolean enabled();
    
    @WithDefault("***")
    String defaultMaskValue();
    
    @WithDefault("CURLY_BRACES")
    FieldPattern fieldPattern();
    
    Map<String, String> customMasks();
    
    enum FieldPattern {
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

@ApplicationScoped
public class MaskMeConfigurationService {
    
    @Inject
    MaskMeConfig config;
    
    @PostConstruct
    public void configure() {
        if (config.enabled()) {
            MaskMeFieldAccessUtil.setUserPattern(config.fieldPattern().getCompiledPattern());
        }
    }
}
```

### Step 4: Field Reference Configuration

Configure custom field reference patterns:

```java
@ApplicationScoped
public class FieldPatternConfiguration {
    
    @ConfigProperty(name = "maskme.field.pattern", defaultValue = "CURLY_BRACES")
    String patternType;
    
    @PostConstruct
    public void configureFieldPattern() {
        Pattern pattern = switch (patternType) {
            case "SQUARE_BRACKETS" -> Pattern.compile("\\[([^]]+)]");
            case "PARENTHESES" -> Pattern.compile("\\(([^)]+)\\)");
            default -> Pattern.compile("\\{([^}]+)\\}");
        };
        
        MaskMeFieldAccessUtil.setUserPattern(pattern);
    }
}
```

### Step 5: Custom Converters

Implement Quarkus-aware custom converters:

```java
@ApplicationScoped
public class QuarkusEmailConverter implements MaskMeConverter {
    
    @Inject
    MaskMeConfig config;
    
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
            String customMask = config.customMasks().get("email");
            return customMask != null ? customMask.replace("{value}", processedValue) : processedValue;
        }
        
        return processedValue;
    }
}
```

## 🎯 REST Resource Integration

### Using MaskMeInitializer (Recommended)

```java
@Path("/api/users")
@ApplicationScoped
public class UserResource {
    
    @Inject
    UserService userService;
    
    @Inject
    UserMapper userMapper;
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getUser(@PathParam("id") Long id,
                          @HeaderParam("X-Mask-Level") @DefaultValue("none") String maskLevel) {
        
        User user = userService.findById(id);
        UserDto dto = userMapper.toDto(user);
        
        return MaskMeInitializer.mask(dto,
            MaskMeOnInput.class, maskLevel,
            RoleBasedMaskCondition.class, "ADMIN"
        );
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserDto> getUsers(@HeaderParam("X-Environment") @DefaultValue("prod") String env) {
        
        return userService.findAll().stream()
            .map(userMapper::toDto)
            .map(dto -> MaskMeInitializer.mask(dto, ConfigBasedCondition.class, env))
            .toList();
    }
}
```

### Using MaskMeProcessor with CDI

```java
@Path("/api/users")
@ApplicationScoped
public class UserResource {
    
    @Inject
    UserService userService;
    
    @Inject
    MaskMeProcessor maskProcessor;
    
    @GET
    @Path("/{id}/detailed")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getDetailedUser(@PathParam("id") Long id,
                                  @QueryParam("maskSensitive") @DefaultValue("false") boolean maskSensitive) {
        
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

## 🔧 Advanced Quarkus Integration

### Request-Scoped Converters with JAX-RS

```java
@Path("/api/advanced")
@ApplicationScoped
public class AdvancedMaskingResource {
    
    @Inject
    UserService userService;
    
    @GET
    @Path("/users/{id}/custom-mask")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getUserWithCustomMask(@PathParam("id") Long id,
                                        @HeaderParam("X-Mask-Pattern") String pattern) {
        
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
```

### Quarkus Security Integration

```java
@ApplicationScoped
public class SecurityAwareMaskCondition implements MaskMeCondition {
    
    @Inject
    SecurityIdentity securityIdentity;
    
    @Inject
    JsonWebToken jwt;
    
    private String requiredRole;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.requiredRole = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        if (securityIdentity.isAnonymous()) {
            return true; // Mask for anonymous users
        }
        
        return !securityIdentity.hasRole(requiredRole);
    }
}

// Usage in resource
@Path("/secure")
@ApplicationScoped
public class SecureResource {
    
    @GET
    @Path("/{id}")
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getSecureUser(@PathParam("id") Long id) {
        User user = userService.findById(id);
        UserDto dto = userMapper.toDto(user);
        
        return MaskMeInitializer.mask(dto, SecurityAwareMaskCondition.class, "ADMIN");
    }
}
```

### Native Compilation Support

For GraalVM native compilation, register reflection classes:

```java
@RegisterForReflection({
    AlwaysMaskMeCondition.class,
    MaskMeOnInput.class,
    RoleBasedMaskCondition.class,
    ConfigBasedCondition.class
})
public class ReflectionConfiguration {
}
```

Or use `reflection-config.json`:

```json
[
  {
    "name": "com.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "com.javamsdt.maskme.implementation.condition.MaskMeOnInput",
    "allDeclaredConstructors": true,
    "allPublicConstructors": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  }
]
```

## 📊 Configuration Properties

### application.properties

```properties
# MaskMe Configuration, this is a custom configuration and not refrecincing anything in the library,
# they are used by you when you configure your application.
maskme.enabled=true
maskme.default-mask-value=***
maskme.field.pattern=CURLY_BRACES

# Custom mask patterns
maskme.custom-masks.email={value}@masked.com
maskme.custom-masks.phone=***-***-****

# Environment-specific settings
%dev.maskme.enabled=false
%test.maskme.enabled=true
%prod.maskme.enabled=true

# Application settings
app.masking.enabled=true
app.environment=prod
```

### application.yml

```yaml
maskme:
  enabled: true
  default-mask-value: "***"
  field:
    pattern: CURLY_BRACES
  custom-masks:
    email: "{value}@masked.com"
    phone: "***-***-****"

"%dev":
  maskme:
    enabled: false

"%test":
  maskme:
    enabled: true

"%prod":
  maskme:
    enabled: true

app:
  masking:
    enabled: true
  environment: prod
```

## 🧪 Testing with Quarkus

### Unit Testing

```java
@QuarkusTest
class MaskingIntegrationTest {
    
    @Inject
    UserService userService;
    
    @TestConfigProperty(name = "maskme.enabled", value = "true")
    @TestConfigProperty(name = "maskme.default-mask-value", value = "TEST_MASK")
    @Test
    void testMaskingWithQuarkusContext() {
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
    void testCustomConditionWithCDI() {
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
@QuarkusTest
class UserResourceTest {
    
    @Test
    void testMaskedEndpoint() {
        given()
            .header("X-Mask-Level", "maskMe")
        .when()
            .get("/api/users/1")
        .then()
            .statusCode(200)
            .body("password", equalTo("****"))
            .body("email", containsString("@masked.com"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = "ADMIN")
    void testSecureEndpointWithAdmin() {
        given()
        .when()
            .get("/secure/1")
        .then()
            .statusCode(200)
            .body("ssn", not(equalTo("***-**-****"))); // Not masked for admin
    }
}
```

### Native Testing

```java
@QuarkusIntegrationTest
class NativeMaskingIT extends MaskingIntegrationTest {
    // Tests run in native mode
}
```

## 📝 Best Practices

### 1. CDI Configuration

- Use `@ApplicationScoped` for singleton conditions
- Inject BeanManager for dynamic bean resolution
- Configure a framework provider once at startup

### 2. Configuration Management

- Use `@ConfigMapping` for type-safe configuration
- Leverage profile-specific settings
- Configure field patterns at startup

### 3. Native Compilation

- Register reflection classes for conditions
- Use `@RegisterForReflection` annotation
- Test native compilation with integration tests

### 4. Resource Design

- Use MaskMeInitializer for cleaner code
- Handle JAX-RS headers for dynamic masking
- Implement proper exception handling

### 5. Memory Management

- Use `@PreDestroy` to clear global converters
- Clear request-scoped converters properly
- Avoid memory leaks with the CDI lifecycle

## ⚠️ Common Issues & Solutions

### Issue 1: CDI Bean Not Found

```java
// Problem: Built-in conditions not found in CDI context
// Solution: Use producer methods
@Produces
@ApplicationScoped
public AlwaysMaskMeCondition alwaysMaskMeCondition() {
    return new AlwaysMaskMeCondition();
}
```

### Issue 2: Native Compilation Failures

```java
// Problem: Reflection not configured for native
// Solution: Register classes for reflection
@RegisterForReflection({
    AlwaysMaskMeCondition.class,
    MaskMeOnInput.class
})
public class ReflectionConfiguration {
}
```

### Issue 3: Configuration Not Loading

```java
// Problem: Configuration properties not injected
// Solution: Use proper configuration mapping
@ConfigMapping(prefix = "maskme")
public interface MaskMeConfig {
    boolean enabled();
}
```

### Issue 4: BeanManager Injection Issues

```java
// Problem: BeanManager not available
// Solution: Use CDI.current() as fallback
private <T> T getCDIBean(Class<T> type) {
    try {
        return CDI.current().select(type).get();
    } catch (Exception e) {
        return null;
    }
}
```

## 🚀 Performance Considerations

### Native Compilation Optimizations

- Register only necessary classes for reflection
- Use build-time configuration when possible
- Minimize runtime reflection usage

### CDI Optimizations

- Use `@ApplicationScoped` for stateless conditions
- Avoid `@RequestScoped` for heavy operations
- Cache frequently used beans

---

[← Back to Main Documentation](../readME.md)
