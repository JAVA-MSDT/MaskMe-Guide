# MaskMe Library - Internal Architecture

[← Back to README](../readME.md)

## 📋 Overview

- This document provides detailed information about the internal architecture and components of the MaskMe library.
- This is intended for developers who want to understand how the library works internally, provide suggestions,
  enhancements, or extend its functionality.

## 🏛️ Project Architecture Diagram

### High-Level Architecture Overview

### Project Structure

```text
maskme/
├── src/
│   ├── main/java/com/javamsdt/maskme/
│   │   ├── api/
│   │   │   ├── annotation/
│   │   │   │   └── MaskMe.java
│   │   │   ├── condition/
│   │   │   │   ├── MaskMeCondition.java
│   │   │   │   ├── MaskMeConditionFactory.java
│   │   │   │   └── MaskMeFrameworkProvider.java
│   │   │   ├── converter/
│   │   │   │   ├── MaskMeConverter.java
│   │   │   │   └── MaskMeConverterRegistry.java
│   │   │   ├── exception/
│   │   │   │   └── MaskMeException.java
│   │   │   ├── processor/
│   │   │   │   └── MaskMeProcessor.java
│   │   │   └── utils/
│   │   │       └── MaskMeFieldAccessUtil.java
│   │   ├── implementation/
│   │   │   ├── condition/
│   │   │   │   ├── AlwaysMaskMeCondition.java
│   │   │   │   └── MaskMeOnInput.java
│   │   │   └── converter/
│   │   │       ├── MaskMeDateTimeConverter.java
│   │   │       ├── MaskMeFallbackConverter.java
│   │   │       ├── MaskMeNumberConverter.java
│   │   │       ├── MaskMePrimitiveConverter.java
│   │   │       ├── MaskMeSpecialTypeConverter.java
│   │   │       └── MaskMeStringConverter.java
│   │   ├── logging/
│   │   │   └── MaskMeLogger.java
│   │   └── MaskMeInitializer.java
│   └── test/java/com/javamsdt/maskme/
│       ├── api/
│       │   ├── annotation/
│       │   │   └── MaskMeTest.java
│       │   ├── condition/
│       │   │   ├── MaskMeConditionFactoryTest.java
│       │   │   └── MaskMeConditionTest.java
│       │   ├── converter/
│       │   │   ├── MaskMeConverterTest.java
│       │   │   └── MaskMeConverterRegistryTest.java
│       │   ├── exception/
│       │   │   └── MaskMeExceptionTest.java
│       │   ├── processor/
│       │   │   └── MaskMeProcessorTest.java
│       │   └── utils/
│       │       └── MaskMeFieldAccessUtilTest.java
│       ├── implementation/
│       │   ├── condition/
│       │   │   ├── AlwaysMaskMeConditionTest.java
│       │   │   └── MaskOnInputTest.java
│       │   └── converter/
│       │       ├── MaskMeDateTimeConverterTest.java
│       │       ├── MaskMeFallbackConverterTest.java
│       │       ├── MaskMeNumberConverterTest.java
│       │       ├── MaskMePrimitiveConverterTest.java
│       │       ├── MaskMeSpecialTypeConverterTest.java
│       │       └── MaskMeStringConverterTest.java
│       ├── logging/
│       │   └── MaskMeLoggerTest.java
│       └── MaskMeInitializerTest.java
├── docs/
│   ├── 01-library-internal-architecture.md
│   ├── 02-spring-framework-guide.md
│   ├── 03-quarkus-framework-guide.md
│   ├── 04-custom-conditions-and-field-patterns.md
│   └── 05-converter.md
├── .github/
│   └── actions/
│       └── maven.yml
├── .gitignore
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
├── LICENSE.txt
├── pom.xml
└── readME.md
```

### Component Interaction Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MASKME PROCESSING FLOW                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. CLIENT CODE                                                             │
│     ┌─────────────────┐     ┌───────────────────┐                           │
│     │ Annotated POJO  │───▶ │ MaskMeInitializer │                           │
│     │ @MaskMe fields  │     │ (Static Facade)   │                           │
│     └─────────────────┘     └───────────────────┘                           │
│                                       │                                     │
│  2. CORE PROCESSING                   ▼                                     │
│     ┌─────────────────────────────────────────────────────────────────┐     │
│     │                    MaskMeProcessor                              │     │
│     │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │     │
│     │  │   Field     │  │ Condition   │  │    Type Conversion      │  │     │
│     │  │ Discovery   │─▶│ Evaluation  │─▶│      & Assignment       │  │     │
│     │  │ (Reflection)│  │   Engine    │  │                         │  │     │
│     │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │     │
│     └─────────────────────────────────────────────────────────────────┘     │
│                                       │                                     │
│  3. CONDITION RESOLUTION              ▼                                     │
│     ┌─────────────────────────────────────────────────────────────────┐     │
│     │              MaskMeConditionFactory                             │     │
│     │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │     │
│     │  │ Framework   │  │  Built-in   │  │      Custom             │  │     │
│     │  │ Provider    │─▶│ Conditions  │  │    Conditions           │  │     │
│     │  │ (DI Support)│  │             │  │                         │  │     │
│     │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │     │
│     └─────────────────────────────────────────────────────────────────┘     │
│                                       │                                     │
│  4. TYPE CONVERSION                   ▼                                     │
│     ┌─────────────────────────────────────────────────────────────────┐     │
│     │                  ConverterRegistry                              │     │
│     │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │     │
│     │  │   Scoped    │  │  Built-in   │  │      Custom             │  │     │
│     │  │ Converters  │─▶│ Converters  │  │    Converters           │  │     │
│     │  │(Thread/Req) │  │             │  │                         │  │     │
│     │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │     │
│     └─────────────────────────────────────────────────────────────────┘     │
│                                       │                                     │
│  5. OUTPUT                            ▼                                     │
│     ┌─────────────────┐                                                     │
│     │ Masked Object   │◀─────────────────────────────────────────────────   │
│     │ (Same Type)     │                                                     │
│     └─────────────────┘                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 🎯 Core Components Architecture

### 1. `@MaskMe` Annotation

```java
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaskMe {
   String DEFAULT_MASK_VALUE = "****";
    Class<? extends MaskCondition>[] conditions();
    String maskValue() default DEFAULT_MASK_VALUE;
}
```

**Purpose**: Marks fields for masking with specified conditions and mask values.

**Key Features**:

- Supports fields and record components.
- Runtime retention for reflection-based processing.
- Multiple condition support through an array.
- Default mask value fallback.

### 2. `MaskMeCondition` Interface

```java
public interface MaskMeCondition {
    boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField);
    
    default void setInput(Object input) {
        // Default implementation
    }
}
```

**Purpose**: Defines the contract for masking conditions.

**Key Features**:

- `shouldMask()`: Core logic to determine if masking should occur.
- `setInput()`: Optional method to receive runtime input.
- Access to both field value and containing object for context-aware decisions.

### 3. `MaskMeProcessor` Class

**Purpose**: The main processing engine that handles masking logic.

**Key Responsibilities**:

- Reflection-based field discovery.
- Condition evaluation.
- Type conversion coordination.
- ThreadLocal management for condition inputs.
- Nested object processing.

**Key Methods**:

- `process(T object)`: Main entry point for masking.
- `setConditionInput(Class<? extends MaskMeCondition> conditionClass, Object input)`: Set condition inputs.
- `clearInputs()`: Clean up ThreadLocal storage.

### 4. `MaskMeInitializer` Class

**Purpose**: Simplified facade for common masking operations.

**Key Features**:

- Static utility methods.
- Automatic ThreadLocal cleanup.
- Varargs support for condition-input pairs.
- Thread-safe operation.

**Benefits**:

- Reduces boilerplate code.
- Prevents memory leaks through automatic cleanup.
- Provides a cleaner API for common use cases.

## 🔄 Type Conversion System

### Converter Architecture

The library uses a modular converter system with the following hierarchy:

```
┌─────────────────────────────────────────────┐
│           CONVERTER EXECUTION ORDER         │
├─────────────────────────────────────────────┤
│ 1. USER CONVERTERS (Priority > 0)           │
│    • Custom business logic                  │
│    • Override default behavior              │
│                                             │
│ 2. DEFAULT CONVERTERS (Priority = 0)        │
│    • PrimitiveConverter                     │
│    • NumberConverter                        │
│    • DateTimeConverter                      │
│    • SpecialTypeConverter                   │
│    • FallbackConverter                      │
│    • StringConverter                        │
└─────────────────────────────────────────────┘
```

### Built-in Converters

#### 1. PrimitiveConverter

- **Handles**: Character, Boolean, primitives, and wrappers
- **Priority**: 0
- **Special Features**: Handles null values and primitive type conversion

#### 2. NumberConverter

- **Handles**: All numeric types (byte, int, long, float, double, BigDecimal, BigInteger)
- **Priority**: 0
- **Special Features**:
    - Blank mask value handling (returns 0 or original value manipulation)
    - Proper scale handling for BigDecimal

#### 3. DateTimeConverter

- **Handles**: All Java 8+ date/time types and legacy Date types
- **Priority**: 0
- **Supported Types**:
    - LocalDate, LocalDateTime, LocalTime
    - Instant, ZonedDateTime, OffsetDateTime
    - Year, YearMonth, MonthDay
    - java.util.Date, java.sql.Date, java.sql.Time, java.sql.Timestamp

#### 4. SpecialTypeConverter

- **Handles**: UUID, File, Path, Enums
- **Priority**: 0
- **Special Features**:
    - Case-insensitive enum conversion
    - Path string to Path object conversion
    - UUID string parsing

#### 5. FallbackConverter

- **Handles**: Any type not handled by other converters
- **Priority**: 0
- **Behavior**: Returns null for unknown types, allowing graceful degradation

#### 6. StringConverter

- **Handles**: String types
- **Priority**: 0
- **Behavior**: Returns null if there is nothing to handle, allowing graceful degradation

### Converter Registry System

#### Scope Hierarchy

```
┌─────────────────────────────────────────────┐
│           CONVERTER SCOPE PRIORITY          │
├─────────────────────────────────────────────┤
│ 1. THREAD-SCOPED (Highest Priority)         │
│    • Test isolation                         │
│    • Background job specific                │
│                                             │
│ 2. REQUEST-SCOPED (Web Applications)        │
│    • User-specific converters               │
│    • Session-based rules                    │
│                                             │
│ 3. GLOBAL-SCOPED (Application-wide)         │
│    • Default application converters         │
│    • Shared business rules                  │
│                                             │
│ 4. DEFAULT CONVERTERS (Lowest Priority)     │
│    • Built-in type converters               │
└─────────────────────────────────────────────┘
```

#### Registry Management

- **Global Registry**: Application-wide converters.
- **Thread-Local Registry**: Thread-specific converters.
- **Request-Scoped Registry**: HTTP request-specific converters.
- **Automatic Cleanup**: Prevents memory leaks through proper scope management.

## 🏗️ Framework Integration Architecture

### Framework Provider Pattern

```java
public interface MaskMeFrameworkProvider {
    <T> T getInstance(Class<T> type);
}
```

**Purpose**: Abstracts framework-specific bean resolution.

**Implementation Examples**:

- Spring: Uses ApplicationContext.getBean().
- CDI: Uses BeanManager.
- Custom: Manual instance creation.

### Condition Factory

```java
public class MaskMeConditionFactory {
  private static volatile MaskMeFrameworkProvider maskMeFrameworkProvider = null;
    
    public static void setFrameworkProvider(MaskMeFrameworkProvider provider) {
        frameworkProvider = provider;
    }
    
    public static <T extends MaskMeCondition> T createCondition(Class<T> conditionClass) {
        // Framework-aware instance creation
    }
}
```

**Key Features**:

- Framework-agnostic condition instantiation.
- Dependency injection support.
- Fallback to reflection-based creation.

## 🔍 Field Processing Architecture

### Field Discovery Process

1. **Class Analysis**: Determine if an object is a Record or regular class.
2. **Field Extraction**:
    - Records: Use record components.
    - Classes: Use declared fields.
3. **Annotation Scanning**: Find fields with @MaskMe annotation.
4. **Nested Object Detection**: Identify fields that need recursive processing.

### Masking Pipeline

```
Input Object
     ↓
Field Discovery
     ↓
For Each @MaskMe Field:
     ↓
Condition Evaluation
     ↓
Should Mask?
     ↓ (Yes)
Mask Value Processing
     ↓
Field Reference Resolution
     ↓
Type Conversion
     ↓
Field Assignment
     ↓
Nested Object Processing
     ↓
Output Masked Object
```

### [Field Reference Resolution](04-custom-conditions-and-field-patterns.md)

The library supports dynamic field referencing using configurable patterns:

#### Default Pattern: `{fieldName}`

- Regex: `\\{([^}]+)\\}`
- Captures field names within curly braces.
- Example: `"{name}@masked.com"` → `"Ahmed@masked.com"`

#### Custom Pattern Support

- Configurable through `MaskMeFieldAccessUtil.setUserPattern()`
- Supports various bracket types: `{}`, `[]`, `()`, `<>`, `[[]]`
- Pattern validation ensures single capturing group.

#### Resolution Process

1. **Pattern Matching**: Find all field references in mask value.
2. **Field Lookup**: Resolve field names to actual values.
3. **Value Substitution**: Replace references with actual field values.
4. **Type Conversion**: Convert the final string to a target field type.

## 🧵 Thread Safety Architecture

### ThreadLocal Management

The library uses ThreadLocal storage for condition inputs to ensure thread safety:

```java
private static final ThreadLocal<Map<Class<? extends MaskMeCondition>, Object>> 
    conditionInputs = new ThreadLocal<>();
```

**Benefits**:

- Thread isolation prevents cross-request contamination.
- No synchronization overhead.
- Automatic cleanup prevents memory leaks.

### Memory Management

#### Automatic Cleanup (MaskMeInitializer)

- ThreadLocal cleanup in finally blocks.
- No manual intervention is required.
- Exception-safe cleanup.

#### Manual Cleanup (MaskMeProcessor)

- Explicit `clearInputs()` call required.
- Developer responsibility.
- More control over cleanup timing.

## 🔧 Reflection Architecture

### Object Creation Strategy

#### Records

- Use canonical constructor.
- Parameter order matching.
- Immutable object creation.

#### Regular Classes

- Default constructor instantiation.
- Field-by-field assignment.
- Setter method utilization when available.

### Performance Considerations

- **No Caching**: Avoids memory leaks but may impact performance.
- **Reflection Overhead**: Minimal due to modern JVM optimizations.
- **Object Creation**: New instances prevent original object modification.

## 🎯 Extension Points

### [Custom Converter Development](05-converter.md)

1. **Implement Converter Interface**
2. **Set Appropriate Priority** (> 0 to override defaults)
3. **Register in Appropriate Scope**
4. **Handle Field Context** (fieldName, originalValue, objectContainingMaskedField)

### [Custom Condition Development](04-custom-conditions-and-field-patterns.md)

1. **Implement MaskMeCondition Interface**
2. **Handle Input Processing** via `setInput()`
3. **Implement Business Logic** in `shouldMask()`
4. **Consider Framework Integration** for dependency injection

### Framework Integration

1. **Implement MaskMeFrameworkProvider**
2. **Register with MaskMeConditionFactory**
3. **Handle Bean Resolution**
4. **Manage Lifecycle** (startup/shutdown).

## 📊 Performance Characteristics

### Time Complexity

- **Field Discovery**: O(n) where n = number of fields.
- **Condition Evaluation**: O(c) where c = number of conditions per field.
- **Type Conversion**: O(1) for most types.
- **Overall**: O(n × c) for flat objects, O(n × c × d) for nested objects (d = depth).

### Space Complexity

- **Memory Usage**: O(n) for object duplication.
- **ThreadLocal Storage**: O(c) per thread for condition inputs.
- **Converter Registry**: O(r) where r = number of registered converters.

### Optimization Strategies

- **Early Exit**: Conditions evaluated in order, first match wins.
- **Switch Expressions**: Modern Java syntax for efficient type matching.
- **Minimal Reflection**: Only when necessary for object creation.

## 🔍 Error Handling Architecture

### Exception Hierarchy

```
MaskMeException (Runtime)
├── Condition Creation Failures
├── Type Conversion Errors
├── Reflection Errors
└── Framework Integration Errors
```

### Error Recovery Strategies

1. **Graceful Degradation**: Return null for unknown types.
2. **Logging**: Warn about non-critical failures.
3. **Fallback Mechanisms**: Use reflection when framework injection fails.
4. **Exception Propagation**: Critical errors bubble up to the caller.

## 🧪 Testing Architecture

### Test Categories

1. **Unit Tests**: Individual component testing.
2. **Integration Tests**: Framework integration testing.
3. **Performance Tests**: Load and stress testing.
4. **Compatibility Tests**: Cross-framework validation.

### Test Isolation

- Thread-local converter registration.
- Automatic cleanup in test teardown.
- Mock framework providers for testing.

---

[← Back to README](../readME.md)