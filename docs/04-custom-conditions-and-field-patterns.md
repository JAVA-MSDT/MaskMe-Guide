# Custom Conditions and Field Patterns Guide

## 📋 Overview

- This guide covers creating custom masking conditions and configuring field reference patterns in the MaskMe library.
- Learn how to implement complex business logic for conditional masking and customize field placeholder syntax.

## 🎯 Custom Masking Conditions

### Understanding MaskMeCondition Interface

All custom conditions must implement the `MaskMeCondition` interface:

```java
public interface MaskMeCondition {
    /**
     * Sets runtime input for the condition
     * @param input Runtime input passed from the processor
     */
    default void setInput(Object input) {
        // Optional: Override if the condition needs runtime input
    }
    
    /**
     * Determines if a field should be masked
     * @param maskedFieldValue Current field value
     * @param objectContainingMaskedField Object containing this field
     * @return true if the field should be masked
     */
    boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField);
}
```

### Built-in Conditions

#### AlwaysMaskMeCondition

```java
public class AlwaysMaskMeCondition implements MaskMeCondition {
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return true; // Always mask
    }
}
```

#### MaskMeOnInput

```java
public class MaskMeOnInput implements MaskMeCondition {
    private static final MaskMeLogger logger = MaskMeLogger.getLogger(MaskMeOnInput.class);

    private String input;
    private static final String EXPECTED_INPUT = "maskMe";
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.input = (String) input;
            logger.debug(() -> "Input set to: " + this.input);
        } else {
            logger.debug(() -> "Invalid input type: " + (input != null ? input.getClass().getSimpleName() : "null"));
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        boolean result = input != null && input.equalsIgnoreCase(EXPECTED_INPUT);
        logger.debug(() -> "evaluation: input='" + input + "', result=" + result);
        return result;
    }
}
```

## 🔧 Creating Custom Conditions

### 1. Simple Custom Condition

```java
public class EnvironmentBasedCondition implements MaskMeCondition {
    private String environment;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.environment = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return "production".equalsIgnoreCase(environment);
    }
}

// Usage
@MaskMe(conditions = {EnvironmentBasedCondition.class}, maskValue = "***")
String sensitiveData;

// In controller
public class DummyController {
    public void dummyMethod() {
        MaskMeInitializer.mask(dto, EnvironmentBasedCondition.class, "production");
    }
}
```

### 2. Role-Based Condition

```java
public class RoleBasedCondition implements MaskMeCondition {
    private String requiredRole;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.requiredRole = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        // Get current user role from security context
        String currentUserRole = getCurrentUserRole();
        return !requiredRole.equals(currentUserRole);
    }
    
    private String getCurrentUserRole() {
        // Implementation depends on your security framework
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse("USER");
    }
}
```

### 3. Field Value-Based Condition

```java
public class ValueBasedCondition implements MaskMeCondition {
    private Object thresholdValue;
    
    @Override
    public void setInput(Object input) {
        this.thresholdValue = input;
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        if (maskedFieldValue instanceof Number && thresholdValue instanceof Number) {
            double maskedFieldVal = ((Number) maskedFieldValue).doubleValue();
            double threshold = ((Number) thresholdValue).doubleValue();
            return maskedFieldVal > threshold;
        }
        return false;
    }
}

// Usage
@MaskMe(conditions = {ValueBasedCondition.class}, maskValue = "***")
BigDecimal salary;

// Mask salaries above 100,000
public class DummyController {
    public void dummyMethod() {
        MaskMeInitializer.mask(dto, ValueBasedCondition.class, 100000);
    }
}
```

### 4. Time-Based Condition

```java
public class TimeBasedCondition implements MaskMeCondition {
    private LocalTime startTime;
    private LocalTime endTime;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String timeRange) {
            String[] times = timeRange.split("-");
            if (times.length == 2) {
                this.startTime = LocalTime.parse(times[0]);
                this.endTime = LocalTime.parse(times[1]);
            }
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        LocalTime now = LocalTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }
}

// Usage - mask during business hours
public class DummyController {
    public void dummyMethod() {
        MaskMeInitializer.mask(dto, TimeBasedCondition.class, "09:00-17:00");
    }
}
```

### 5. Complex Business Logic Condition

```java
@Component // Spring-managed for dependency injection
public class BusinessRuleCondition implements MaskMeCondition {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ConfigurationService configService;
    
    private String ruleType;
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.ruleType = (String) input;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return switch (ruleType) {
            case "GDPR_COMPLIANCE" -> isGdprApplicable(objectContainingMaskedField);
            case "AUDIT_MODE" -> configService.isAuditModeEnabled();
            case "USER_CONSENT" -> !hasUserConsent(objectContainingMaskedField);
            default -> false;
        };
    }
    
    private boolean isGdprApplicable(Object objectContainingMaskedField) {
        if (objectContainingMaskedField instanceof UserDto user) {
            return userService.isEuResident(user.getId());
        }
        return false;
    }
    
    private boolean hasUserConsent(Object objectContainingMaskedField) {
        if (objectContainingMaskedField instanceof UserDto user) {
            return userService.hasDataProcessingConsent(user.getId());
        }
        return false;
    }
}
```

## 🔗 Field Reference Patterns

### Default Pattern: Curly Braces `{fieldName}`

```java
public record UserDto(
    @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "{firstName}@masked.com")
    String email,
    
    @MaskMe(conditions = {MaskMeOnInput.class}, maskValue = "{id}-{department}")
    String displayName,
    
    String firstName,
    Long id,
    String department
) {}
```

### Configuring Custom Patterns

#### 1. Application Startup Configuration

```java
@Configuration
public class MaskMeConfiguration {
    
    @PostConstruct
    public void configureFieldPattern() {
        // Set a custom pattern at application startup
        MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\[([^]]+)]"));
    }
}
```

#### 2. Available Pattern Options

| Pattern Type               | Regex Pattern      | Java String                | Example Usage   |
|----------------------------|--------------------|----------------------------|-----------------|
| **Curly Braces** (Default) | `\\{([^}]+)\\}`    | `"\\\\{([^}]+)\\\\}"`      | `{fieldName}`   |
| **Square Brackets**        | `\\[([^]]+)]`      | `"\\\\[([^]]+)]"`          | `[fieldName]`   |
| **Parentheses**            | `\\(([^)]+)\\)`    | `"\\\\(([^)]+)\\\\)"`      | `(fieldName)`   |
| **Angle Brackets**         | `<([^>]+)>`        | `"<([^>]+)>"`              | `<fieldName>`   |
| **Double Square**          | `\\[\\[([^]]+)]]`  | `"\\\\[\\\\[([^]]+)]]"`    | `[[fieldName]]` |
| **Dollar Sign**            | `\\$\\{([^}]+)\\}` | `"\\\\$\\\\{([^}]+)\\\\}"` | `${fieldName}`  |

#### 3. Pattern Configuration Examples

```java
// Square brackets pattern
public class OnApplicationStartup {
    public void initializedOnlyOnce() {
        MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\[([^]]+)]"));
    }
}

public record UserDto(
        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "[name]@company.com")
        String email,
        String name
) {
}

// Dollar sign pattern (Spring-like)
public class OnApplicationStartup {
    public void initializedOnlyOnce() {
        MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\$\\{([^}]+)\\}"));
    }
}

public record UserDto(
        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "${firstName}_${lastName}")
        String username,
        String firstName,
        String lastName
) {
}
```

### Advanced Field Referencing

#### 1. Multiple Field References

```java
public record EmployeeDto(
    @MaskMe(conditions = {AlwaysMaskMeCondition.class}, 
            maskValue = "{firstName}.{lastName}@{company}.com")
    String email,
    
    @MaskMe(conditions = {MaskMeOnInput.class}, 
            maskValue = "{department}-{id}-{level}")
    String employeeCode,
    
    String firstName,
    String lastName,
    String company,
    String department,
    Long id,
    String level
) {}
```

#### 2. Nested Object Field References

```java
public record OrderDto(
    @MaskMe(conditions = {AlwaysMaskMeCondition.class}, 
            maskValue = "Order-{id} for {customer}")
    String description,
    
    Long id,
    String customer,
    AddressDto shippingAddress
) {}

// Field references work within the same object level
// For nested object access, use custom converters
```

## 🎯 Best Practices

### 1. Condition Design

```java
// ✅ Good - Clear, focused responsibility
public class AdminOnlyCondition implements MaskMeCondition {
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return !isCurrentUserAdmin();
    }
}

// ❌ Avoid - Multiple responsibilities
public class ComplexCondition implements MaskMeCondition {
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return !isCurrentUserAdmin() && 
               isProductionEnvironment() && 
               isBusinessHours() && 
               hasGdprRequirement();
    }
}
```

### 2. Input Validation

```java
public class SafeCondition implements MaskMeCondition {
    private String requiredValue;
    
    @Override
    public void setInput(Object input) {
        // Always validate input
        if (input instanceof String && !((String) input).trim().isEmpty()) {
            this.requiredValue = ((String) input).trim().toLowerCase();
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return requiredValue != null && requiredValue.equals("mask");
    }
}
```

### 3. Framework Integration

```java
// Spring Integration
@Component
public class SpringManagedCondition implements MaskMeCondition {
    
    @Autowired
    private SecurityService securityService;
    
    @Value("${app.masking.enabled:true}")
    private boolean maskingEnabled;
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        return maskingEnabled && !securityService.hasAdminRole();
    }
}

// Register as a bean in configuration
@Bean
public SpringManagedCondition springManagedCondition() {
    return new SpringManagedCondition();
}
```

### 4. Performance Considerations

```java
public class OptimizedCondition implements MaskMeCondition {
    private static final String ADMIN_ROLE = "ADMIN";
    private String userRole;
    
    @Override
    public void setInput(Object input) {
        // Cache expensive operations in setInput
        this.userRole = getCurrentUserRole();
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        // Fast comparison in shouldMask
        return !ADMIN_ROLE.equals(userRole);
    }
}
```

## 🧪 Testing Custom Conditions

### Unit Testing

```java
@Test
void testRoleBasedCondition() {
    // Given
    RoleBasedCondition condition = new RoleBasedCondition();
    condition.setInput("ADMIN");
    
    // When
    boolean shouldMask = condition.shouldMask("sensitive", new UserDto());
    
    // Then
    assertThat(shouldMask).isFalse(); // Admin should not be masked
}

@Test
void testFieldReferencePattern() {
    // Given
    UserDto user = new UserDto("Ahmed", "Ahmed@email.com");
    
    // When
    UserDto masked = MaskMeInitializer.mask(user);
    
    // Then
    assertThat(masked.email()).isEqualTo("Ahmed@masked.com");
}
```

### Integration Testing

```java
@SpringBootTest
class CustomConditionIntegrationTest {
    
    @Test
    @WithMockUser(roles = "USER")
    void testSpringSecurityIntegration() {
        // Given
        UserDto dto = new UserDto("sensitive data");
        
        // When
        UserDto masked = MaskMeInitializer.mask(dto, 
            SpringSecurityCondition.class, "ADMIN");
        
        // Then
        assertThat(masked.data()).isEqualTo("***"); // Masked for non-admin
    }
}
```

## ⚠️ Common Pitfalls

### 1. Thread Safety

```java
// ❌ Not thread-safe
public class UnsafeCondition implements MaskMeCondition {
    private static String globalInput; // Shared state
    
    @Override
    public void setInput(Object input) {
        globalInput = (String) input; // Race condition
    }
}

// ✅ Thread-safe
public class SafeCondition implements MaskMeCondition {
    private String instanceInput; // Instance state
    
    @Override
    public void setInput(Object input) {
        this.instanceInput = (String) input; // Safe
    }
}
```

### 2. Field Reference Limitations

```java
// ❌ Cannot reference nested object fields directly
@MaskMe(conditions = {AlwaysMaskMeCondition.class}, 
        maskValue = "{address.street}") // Won't work
String description;

// ✅ Use custom converter for complex references
@MaskMe(conditions = {AlwaysMaskMeCondition.class}, 
        maskValue = "CUSTOM_LOGIC")
String description;
```

### 3. Input Type Safety

```java
// ❌ Unsafe casting
@Override
public void setInput(Object input) {
    this.value = (String) input; // ClassCastException risk
}

// ✅ Safe type checking
@Override
public void setInput(Object input) {
    if (input instanceof String) {
        this.value = (String) input;
    }
}
```

---

[← Back to Main Documentation](../readME.md)
