# Quarkus Framework Integration Guide

## 📋 Overview

- This guide demonstrates how to integrate the MaskMe library with Quarkus applications.
- Leveraging CDI (Contexts and Dependency Injection), configuration management, and native compilation support.

## 🚀 Running the Application

### Prerequisites

- Java 21+
- Maven 3.6+

### Start the Application

```bash
# Clone and navigate to the project
cd Quarkus-maskme

# Build and run
mvn clean install
mvn quarkus:dev
```

The application starts on `http://localhost:9091`

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
curl http://localhost:9091/users/1

# 2. Get user with masking enabled
curl -H "Mask-Input: maskMe" http://localhost:9091/users/masked/1

# 3. Get all users with phone masking for specific number
curl -H "Mask-Input: maskMe" -H "Mask-Phone: 01000000000" http://localhost:9091/users

# 4. Get domain entity (always masks certain fields)
curl http://localhost:9091/users/user/1
```

## 🔧 Configuration Setup

### Step 1: Framework Configuration

Configure MaskMe with Quarkus CDI for dependency injection support:

```java
@ApplicationScoped
public class MaskMeConfiguration {

    void onStart(@Observes StartupEvent ev) {
        registerFrameworkProvider();
        setupCustomConverters();
    }

    private void registerFrameworkProvider() {
        MaskMeConditionFactory.setFrameworkProvider(new MaskMeFrameworkProvider() {
            @Override
            public <T> T getInstance(Class<T> type) {
                try {
                    return CDI.current().select(type).get();
                } catch (Exception e) {
                    return null; // Let library fall back to reflection
                }
            }
        });
    }

    @Produces
    @ApplicationScoped
    @Unremovable  // CRITICAL: Prevents Quarkus from removing unused beans
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }

    @Produces
    @ApplicationScoped
    @Unremovable  // CRITICAL: Prevents Quarkus from removing unused beans
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }

    private void setupCustomConverters() {
        MaskMeConverterRegistry.clearGlobal();
        MaskMeConverterRegistry.registerGlobal(new CustomStringConverter());
    }

    void onStop(@Observes io.quarkus.runtime.ShutdownEvent ev) {
        MaskMeConverterRegistry.clearGlobal();
    }
}
```

### Step 2: Custom Conditions with CDI

Create CDI-managed conditions with dependency injection.

**Option 1: Using @Unremovable on the class (Recommended)**

```java
@ApplicationScoped
@Unremovable  // Prevents Quarkus from removing this bean
public class PhoneMaskingCondition implements MaskMeCondition {
    
    private final UserService userService;
    private String input;
    
    public PhoneMaskingCondition(UserService userService) {
        this.userService = userService;
    }
    
    @Override
    public void setInput(Object input) {
        if (input instanceof String inputValue) {
            this.input = inputValue;
        }
    }
    
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        boolean anyUserPhoneMatched = userService.findUsers().stream()
                .anyMatch(user -> user.getPhone().equals(maskedFieldValue));
        
        return input != null && input.equals(maskedFieldValue) && anyUserPhoneMatched;
    }
}
```

**Option 2: Using @Produces with @Unremovable in configuration**

```java
// In MaskMeConfiguration class
@Produces
@ApplicationScoped
@Unremovable
public PhoneMaskingCondition phoneMaskingCondition(UserService userService) {
    return new PhoneMaskingCondition(userService);
}

// PhoneMaskingCondition class (no CDI annotations needed)
public class PhoneMaskingCondition implements MaskMeCondition {
    private final UserService userService;
    private String input;
    
    public PhoneMaskingCondition(UserService userService) {
        this.userService = userService;
    }
    // ... rest of implementation
}
```

### Step 3: REST Resource Integration

```java
@Path("/users")
public class UserResource {
    
    @Inject
    UserService userService;
    
    @Inject
    UserMapper userMapper;
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getUserById(@PathParam("id") Long id) {
        return userMapper.toDto(userService.findUserById(id));
    }
    
    @GET
    @Path("/masked/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getMaskedUserById(@PathParam("id") Long id, 
                                     @HeaderParam("Mask-Input") String maskInput) {
        UserDto userDto = userMapper.toDto(userService.findUserById(id));
        return MaskMeInitializer.mask(userDto, MaskMeOnInput.class, maskInput);
    }
}
```

## 📝 Best Practices

### 1. CDI Configuration

- Use `@ApplicationScoped` for singleton conditions
- **CRITICAL**: Always add `@Unremovable` to condition beans (Quarkus removes unused beans at build time)
- Use `CDI.current().select(type).get()` for programmatic bean lookup
- Configure framework provider at startup using `@Observes StartupEvent`

### 2. Bean Discovery

- Quarkus optimizes by removing beans not directly injected
- Beans looked up programmatically need `@Unremovable`
- Alternative: Add `META-INF/beans.xml` with `bean-discovery-mode="all"`

### 3. Resource Design

- Use MaskMeInitializer for cleaner code
- Handle JAX-RS headers for dynamic masking
- Implement proper exception handling

### 4. Memory Management

- Use `@Observes ShutdownEvent` to clear global converters
- Clear request-scoped converters properly
- Avoid memory leaks with CDI lifecycle

## ⚠️ Common Issues & Solutions

### Issue 1: "No bean found for required type" Error

**Problem**: Quarkus removes beans at build time that aren't directly injected.

**Solution**: Add `@Unremovable` annotation:

```java
// Option 1: On the class
@ApplicationScoped
@Unremovable
public class PhoneMaskingCondition implements MaskMeCondition { }

// Option 2: On producer method
@Produces
@ApplicationScoped
@Unremovable
public AlwaysMaskMeCondition alwaysMaskMeCondition() {
    return new AlwaysMaskMeCondition();
}
```

### Issue 2: Beans Marked as Unused During Build

**Error Message**:

```
CDI: programmatic lookup problem detected
At least one bean matched the required type but was marked as unused and removed during build
```

**Solution**: This confirms you need `@Unremovable`. See Issue 1.

### Issue 3: Constructor Injection Not Working

**Problem**: Field injection with `@Inject` doesn't work when beans are created programmatically.

**Solution**: Use constructor injection with producer methods:

```java
@Produces
@ApplicationScoped
@Unremovable
public PhoneMaskingCondition phoneMaskingCondition(UserService userService) {
    return new PhoneMaskingCondition(userService);
}
```

### Issue 4: Framework Provider Returns Null

**Solution**: Ensure beans are registered and return null on exception to allow fallback:

```java
private void registerFrameworkProvider() {
    MaskMeConditionFactory.setFrameworkProvider(new MaskMeFrameworkProvider() {
        @Override
        public <T> T getInstance(Class<T> type) {
            try {
                return CDI.current().select(type).get();
            } catch (Exception e) {
                return null; // Allow library to fall back to reflection
            }
        }
    });
}
```

## 🔑 Key Differences: Quarkus vs Spring

| Aspect                    | Spring                             | Quarkus                                    |
|---------------------------|------------------------------------|--------------------------------------------|
| **Bean Registration**     | `@Bean` methods                    | `@Produces` methods                        |
| **Bean Lookup**           | `applicationContext.getBean(type)` | `CDI.current().select(type).get()`         |
| **Bean Lifecycle**        | All beans kept at runtime          | Unused beans removed at build time         |
| **Programmatic Lookup**   | Works automatically                | Requires `@Unremovable`                    |
| **Constructor Injection** | Works with `@Component`            | Works with `@ApplicationScoped` + producer |
| **Optimization**          | Runtime optimization               | Build-time optimization                    |

**Why the difference?**

- Spring keeps all beans for runtime flexibility
- Quarkus removes unused beans for faster startup and lower memory usage
- Quarkus optimizes for cloud-native and serverless deployments

---

**Happy Masking with Quarkus! 🔒**

## 🧪 Testing with Quarkus

### Running the Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserResourceIntegrationTest

# Run with verbose output
mvn test -X
```

### Test Coverage

The `UserResourceIntegrationTest` covers all key MaskMe features:

| Test                                            | What It Verifies                                         |
|-------------------------------------------------|----------------------------------------------------------|
| `shouldReturnOriginalUserData`                  | Unmasked endpoint returns original data                  |
| `shouldMaskSensitiveFieldsWhenConditionMatches` | MaskMeOnInput condition masks fields correctly           |
| `shouldNotMaskWhenConditionDoesNotMatch`        | Conditions only trigger with matching input              |
| `shouldMaskOnlyMatchingPhoneNumber`             | Custom PhoneMaskingCondition works with CDI              |
| `shouldAlwaysMaskFieldsOnDomainEntity`          | AlwaysMaskMeCondition masks without input                |
| `shouldNotProcessExcludedNestedObject`          | @ExcludeMaskMe prevents nested processing                |
| `shouldReplacePlaceholdersWithFieldValues`      | Field referencing {fieldName} works                      |
| `shouldUseCustomConverterWithHigherPriority`    | Custom converters override defaults                      |
| `shouldConvertMaskValuesToCorrectTypes`         | Type conversion for Long, LocalDate, BigDecimal, Instant |
| `shouldRecursivelyMaskNestedObjectFields`       | Nested object masking works recursively                  |

### Expected Test Output

```bash
$ mvn test -Dtest=UserResourceIntegrationTest

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.javamsdt.masking.controller.UserResourceIntegrationTest
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
@QuarkusTest
class MyMaskingTest {
    
    @Test
    void testCustomMasking() {
        UserDto dto = given()
                .header("Your-Header", "value")
                .when()
                .get("/your-endpoint")
                .then()
                .statusCode(200)
                .extract()
                .as(UserDto.class);
        
        assertThat(dto.maskedField()).isEqualTo("expected-masked-value");
    }
}
```
