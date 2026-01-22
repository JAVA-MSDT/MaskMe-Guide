# Pure Java Integration Guide

## 📋 Overview

- This guide demonstrates how to integrate the MaskMe library with pure Java applications.
- No framework dependencies - simple, lightweight, and portable.
- Manual dependency injection using a Map-based registry.

## 🚀 Running the Application

### Prerequisites

- Java 21+
- Maven 3.6+

### Build and Run

```bash
# Clone and navigate to the project
cd Pure-java-maskme

# Build
mvn clean install

# Run
java -cp target/Pure-java-maskme-0.0.1-SNAPSHOT.jar com.javamsdt.javamasking.JavaMaskingApplication
```

### Demo Scenarios

The application demonstrates 4 masking scenarios:

1. **Get User By ID (No Masking)** - Returns original data
2. **Get Masked User By ID** - Conditional masking with MaskMeOnInput
3. **Get User Entity (Always Masked)** - AlwaysMaskMeCondition on domain entity
4. **Get All Users (Multiple Conditions)** - Combined MaskMeOnInput + PhoneMaskingCondition

### Expected Output

```
============================================================
MaskMe Pure Java Integration Demo
============================================================

=== Get User By ID (No Masking) ===
UserDto[id=1, name=Ahmed Samy, email=one@mail.com, ...]

=== Get Masked User By ID ===
Mask Input: maskMe
UserDto[id=1000, name=one@mail.com-M, email=[EMAIL PROTECTED], password=************, ...]

=== Get User Entity (Always Masked) ===
User[id=1, email=****, password=************, balance=0, ...]

=== Get All Users (Multiple Conditions) ===
Mask Input: maskMe
Mask Phone: 01000000000
UserDto[phone=****, ...]
UserDto[phone=01000000011, ...]
UserDto[phone=01000000022, ...]
UserDto[phone=01000000033, ...]
============================================================
```

## 🔧 Configuration Setup

### Step 1: Pure Java Configuration

Configure MaskMe without any framework:

```java
public class MaskMeConfiguration {

    private static final Map<Class<?>, Object> instances = new HashMap<>();

    public static void setupMaskMe(UserService userService) {
        // Logger configuration
        MaskMeLogger.enable(Level.INFO);

        // Register condition instances for dependency injection
        registerConditionInstances(userService);

        // Register framework provider for pure Java
        registerFrameworkProvider();

        // Setup custom converters
        setupCustomConverters();
    }

    private static void registerConditionInstances(UserService userService) {
        // Register PhoneMaskingCondition with UserService dependency
        instances.put(PhoneMaskingCondition.class, new PhoneMaskingCondition(userService));
    }

    private static void registerFrameworkProvider() {
        MaskMeConditionFactory.setFrameworkProvider(new MaskMeFrameworkProvider() {
            @Override
            public <T> T getInstance(Class<T> type) {
                return (T) instances.get(type);
            }
        });
    }

    private static void setupCustomConverters() {
        MaskMeConverterRegistry.clearGlobal();
        MaskMeConverterRegistry.registerGlobal(new CustomStringConverter());
    }

    public static void destroy() {
        MaskMeConverterRegistry.clearGlobal();
        instances.clear();
    }
}
```

### Step 2: Custom Conditions with Manual DI

Create conditions with constructor-based dependency injection:

```java
public class PhoneMaskingCondition implements MaskMeCondition {

    private final UserService userService;
    private String input;

    public PhoneMaskingCondition(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        boolean anyUserPhoneMatched = userService.findUsers().stream()
                .anyMatch(user -> user.getPhone().equals(maskedFieldValue));

        return input != null
                && input.equals(maskedFieldValue)
                && anyUserPhoneMatched;
    }

    @Override
    public void setInput(Object input) {
        if (input instanceof String inputValue) {
            this.input = inputValue;
        }
    }
}
```

### Step 3: Application Setup

Initialize and use masking in your application:

```java
public class JavaMaskingApplication {

    public static void main(String[] args) {
        // Initialize services
        UserService userService = new UserService();
        UserMasking userMasking = new UserMasking(userService);

        // Configure MaskMe library
        MaskMeConfiguration.setupMaskMe(userService);

        // Use masking
        userMasking.getUserById(1L);
        userMasking.getMaskedUserById(1L, "maskMe");
        userMasking.getUserEntity(1L);
        userMasking.getUsers("maskMe", "01000000000");
    }
}
```

### Step 4: Using MaskMe in Your Code

```java
public class UserMasking {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserMasking(UserService userService) {
        this.userService = userService;
        this.userMapper = new UserMapperImpl();
    }

    public void getUserById(Long id) {
        UserDto userDto = userMapper.toDto(userService.findUserById(id));
        System.out.println(userDto);
    }

    public void getMaskedUserById(Long id, String maskInput) {
        UserDto userDto = userMapper.toDto(userService.findUserById(id));
        UserDto masked = MaskMeInitializer.mask(userDto, MaskMeOnInput.class, maskInput);
        System.out.println(masked);
    }

    public void getUserEntity(Long id) {
        User user = userService.findUserById(id);
        User masked = MaskMeInitializer.mask(user);
        System.out.println(masked);
    }

    public void getUsers(String maskInput, String maskPhone) {
        List<UserDto> users = userService.findUsers().stream()
                .map(user -> MaskMeInitializer.mask(userMapper.toDto(user),
                        MaskMeOnInput.class, maskInput,
                        PhoneMaskingCondition.class, maskPhone))
                .toList();
        users.forEach(System.out::println);
    }
}
```

## 📝 Best Practices

### 1. Manual Dependency Injection

- Use a Map-based registry for condition instances
- Register all conditions with dependencies at startup
- Keep the registry simple and focused

### 2. Memory Management

- Always call `destroy()` when shutting down
- Clear global converters to prevent memory leaks
- Clear the instance registry

### 3. Service Design

- Keep services stateless when possible
- Use constructor injection for dependencies
- Initialize all services before configuring MaskMe

### 4. Testing

- Set up MaskMe configuration in `@BeforeAll`
- Use the same configuration across all tests
- Clean up resources after tests if needed

## ⚠️ Common Issues & Solutions

### Issue 1: Condition Not Found

**Problem**: Custom condition returns null from framework provider.

**Solution**: Register the condition instance in the Map:

```java
private static void registerConditionInstances(UserService userService) {
    instances.put(PhoneMaskingCondition.class, new PhoneMaskingCondition(userService));
}
```

### Issue 2: NullPointerException in Condition

**Problem**: Injected dependency is null.

**Solution**: Ensure the condition is created with dependencies:

```java
// Wrong
instances.put(PhoneMaskingCondition.class, new PhoneMaskingCondition());

// Correct
instances.put(PhoneMaskingCondition.class, new PhoneMaskingCondition(userService));
```

### Issue 3: Memory Leaks

**Problem**: Converters or instances remain in memory after application shutdown.

**Solution**: Always call destroy():

```java
public static void destroy() {
    MaskMeConverterRegistry.clearGlobal();
    instances.clear();
}
```

## 🔑 Key Differences: Pure Java vs Framework

| Aspect                     | Pure Java                 | Spring/Quarkus                    |
|----------------------------|---------------------------|-----------------------------------|
| **Dependency Injection**   | Manual Map-based registry | Automatic via framework           |
| **Bean Management**        | Manual instance creation  | Framework-managed beans           |
| **Configuration**          | Static methods            | @Configuration/@ApplicationScoped |
| **Condition Registration** | Manual Map.put()          | @Component/@Produces              |
| **Complexity**             | Simple, explicit          | More abstraction                  |
| **Dependencies**           | Zero framework deps       | Framework required                |

**When to use Pure Java:**

- Lightweight applications
- No framework dependencies desired
- Simple use cases
- Learning/prototyping
- Embedded systems

**When to use Framework:**

- Large applications
- Complex dependency graphs
- Need framework features (web, security, etc.)
- Team familiar with framework

## 🧪 Testing with Pure Java

### Running the Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserMaskingTest

# Run with verbose output
mvn test -X
```

### Test Coverage

The `UserMaskingTest` covers all key MaskMe features:

| Test                                            | What It Verifies                                         |
|-------------------------------------------------|----------------------------------------------------------|
| `shouldReturnOriginalUserData`                  | Unmasked data returns original values                    |
| `shouldMaskSensitiveFieldsWhenConditionMatches` | MaskMeOnInput condition masks correctly                  |
| `shouldNotMaskWhenConditionDoesNotMatch`        | Conditions only trigger with matching input              |
| `shouldMaskOnlyMatchingPhoneNumber`             | Custom PhoneMaskingCondition works with DI               |
| `shouldAlwaysMaskFieldsOnDomainEntity`          | AlwaysMaskMeCondition masks without input                |
| `shouldNotProcessExcludedField`                 | @ExcludeMaskMe prevents processing                       |
| `shouldReplacePlaceholdersWithFieldValues`      | Field referencing {fieldName} works                      |
| `shouldUseCustomConverterWithHigherPriority`    | Custom converters override defaults                      |
| `shouldConvertMaskValuesToCorrectTypes`         | Type conversion for Long, LocalDate, BigDecimal, Instant |
| `shouldRecursivelyMaskNestedObjectFields`       | Nested object masking works recursively                  |

### Expected Test Output

```bash
$ mvn test -Dtest=UserMaskingTest

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.javamsdt.javamasking.masking.UserMaskingTest
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
@DisplayName("My Custom Masking Test")
class MyMaskingTest {
    
    private static UserService userService;
    
    @BeforeAll
    static void setup() {
        userService = new UserService();
        MaskMeConfiguration.setupMaskMe(userService);
    }
    
    @Test
    void testCustomMasking() {
        UserDto dto = new UserDto(...);
        UserDto masked = MaskMeInitializer.mask(dto, MyCondition.class, "input");
        
        assertThat(masked.field()).isEqualTo("expected-value");
    }
}
```

---

**Happy Masking with Pure Java! 🔒**
