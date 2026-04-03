# MaskMe Benchmark Project

A Spring Boot demo application and benchmark suite comparing the [MaskMe](https://github.com/JAVA-MSDT/MaskMe)
annotation-driven masking library against hardcoded masking. Built for presentations and live demos.

## Table of Contents

- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Web UI](#web-ui)
- [REST API Endpoints](#rest-api-endpoints)
- [Benchmark](#benchmark)
- [Benchmark Methodology](#benchmark-methodology)
- [Benchmark Results](#benchmark-results)
- [Real-World Context](#real-world-context)
- [Tech Stack](#tech-stack)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+

### Run the Application (Web UI + API)

```bash
cd MaskMe-Benchmark
mvn clean install
mvn spring-boot:run
```

Open [http://localhost:9090](http://localhost:9090) to access the web UI.

### Run the CLI Benchmark

```bash
mvn exec:java
```

> **Note:** The CLI benchmark starts its own Spring context on port 9090. Stop the web app first if it's running.

## Project Structure

```
MaskMe-Benchmark/
├── src/main/java/com/javamsdt/
│   ├── benchmark/
│   │   └── UnifiedBenchmark.java          # CLI benchmark (hardcoded vs MaskMe)
│   ├── hardcoded/
│   │   ├── benchmark/
│   │   │   └── HardcodedBenchmark.java    # Standalone hardcoded benchmark
│   │   └── dto/
│   │       ├── AddressDto.java            # Hardcoded masking via .mask() method
│   │       └── UserDto.java               # Hardcoded masking via .mask() method
│   └── masking/
│       ├── controller/
│       │   ├── BenchmarkController.java   # GET /benchmark — live benchmark endpoint
│       │   └── UserController.java        # GET /users — masking demo endpoints
│       ├── domain/
│       │   ├── Address.java               # Domain entity with @MaskMe annotations
│       │   ├── Gender.java
│       │   ├── GeoLocation.java
│       │   └── User.java                  # Domain entity with @MaskMe + @ExcludeMaskMe
│       ├── dto/
│       │   ├── AddressDto.java            # DTO with @MaskMe on city, zipCode
│       │   ├── GeoLocationDto.java        # DTO with @MaskMe on id, latitude
│       │   └── UserDto.java               # DTO with @MaskMe on 8 fields + @ExcludeMaskMe
│       ├── mapper/
│       │   └── UserMapper.java            # MapStruct mapper (User → UserDto)
│       ├── maskme/
│       │   ├── benchmark/
│       │   │   └── SimpleBenchmark.java   # Standalone MaskMe benchmark
│       │   ├── condition/
│       │   │   └── PhoneMaskingCondition.java  # Custom condition with Spring DI
│       │   ├── config/
│       │   │   └── MaskMeConfiguration.java    # Spring config for MaskMe
│       │   └── converter/
│       │       └── CustomStringConverter.java  # Custom converter (email, password)
│       ├── service/
│       │   └── UserService.java           # In-memory user data
│       └── SpringMaskingApplication.java  # Spring Boot entry point
├── src/main/resources/
│   ├── static/
│   │   ├── css/style.css                  # Shared styles for all pages
│   │   ├── index.html                     # Overview page — library description
│   │   ├── benchmarks.html                # Benchmark page — live + reference results
│   │   └── api-demo.html                  # API demo — interactive endpoint testing
│   └── application.properties             # server.port=9090
├── RECOMMENDATIONS.md                     # Detailed analysis and recommendations
├── pom.xml
└── README.md
```

## Architecture

### Application Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot Application                      │
│                     (port 9090)                                   │
├──────────────────────┬──────────────────────────────────────────┤
│                      │                                           │
│   Static HTML Pages  │  REST Controllers                         │
│   ┌────────────────┐ │  ┌──────────────────────────────────┐    │
│   │ index.html     │ │  │ UserController                   │    │
│   │ benchmarks.html│ │  │  GET /users/{id}      (unmasked) │    │
│   │ api-demo.html  │ │  │  GET /users/masked/{id} (masked) │    │
│   └────────────────┘ │  │  GET /users/user/{id}  (domain)  │    │
│                      │  │  GET /users            (all)      │    │
│                      │  ├──────────────────────────────────┤    │
│                      │  │ BenchmarkController              │    │
│                      │  │  GET /benchmark        (live)     │    │
│                      │  └──────────────────────────────────┘    │
├──────────────────────┴──────────────────────────────────────────┤
│                                                                  │
│   MaskMe Library Integration                                     │
│   ┌────────────────────────────────────────────────────────┐    │
│   │ MaskMeConfiguration                                    │    │
│   │  ├─ Framework Provider (Spring ApplicationContext)      │    │
│   │  ├─ Bean Registration (AlwaysMask, MaskMeOnInput)      │    │
│   │  └─ Custom Converter (CustomStringConverter)           │    │
│   ├────────────────────────────────────────────────────────┤    │
│   │ Conditions                                             │    │
│   │  ├─ AlwaysMaskMeCondition  → always masks              │    │
│   │  ├─ MaskMeOnInput          → masks when input="maskMe" │    │
│   │  └─ PhoneMaskingCondition  → masks specific phone      │    │
│   ├────────────────────────────────────────────────────────┤    │
│   │ Annotated DTOs                                         │    │
│   │  ├─ UserDto    (11 fields, 8 masked)                   │    │
│   │  ├─ AddressDto (7 fields, 2 masked)                    │    │
│   │  └─ GeoLocationDto (3 fields, 2 masked)               │    │
│   └────────────────────────────────────────────────────────┘    │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Benchmark Engine                                               │
│   ┌────────────────────────────────────────────────────────┐    │
│   │ UnifiedBenchmark (CLI)                                 │    │
│   │  ├─ Hardcoded masking (UserDto.mask())                 │    │
│   │  └─ MaskMe masking (MaskMeInitializer.mask())          │    │
│   ├────────────────────────────────────────────────────────┤    │
│   │ BenchmarkController (HTTP)                             │    │
│   │  └─ Same methodology, exposed as REST endpoint         │    │
│   └────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

### Masking Annotations Map

| Field      | DTO            | Condition             | Mask Value                      |
|------------|----------------|-----------------------|---------------------------------|
| id         | UserDto        | AlwaysMaskMeCondition | `"1000"`                        |
| name       | UserDto        | MaskMeOnInput         | `"{email}-{genderId}"`          |
| email      | UserDto        | AlwaysMaskMeCondition | `""` (empty → custom converter) |
| password   | UserDto        | AlwaysMaskMeCondition | `"****"` (default)              |
| phone      | UserDto        | PhoneMaskingCondition | `"****"` (default)              |
| birthDate  | UserDto        | AlwaysMaskMeCondition | `"01/01/1800"`                  |
| balance    | UserDto        | AlwaysMaskMeCondition | `""` (empty → 0)                |
| createdAt  | UserDto        | AlwaysMaskMeCondition | `"1900-01-01T00:00:00.00Z"`     |
| genderName | UserDto        | `@ExcludeMaskMe`      | Skipped entirely                |
| city       | AddressDto     | AlwaysMaskMeCondition | `"****"` (default)              |
| zipCode    | AddressDto     | MaskMeOnInput         | `"[ZIP_MASKED]"`                |
| id         | GeoLocationDto | AlwaysMaskMeCondition | `"00000000-0000-..."`           |
| latitude   | GeoLocationDto | AlwaysMaskMeCondition | `"00.0000"`                     |

## Web UI

The application serves three HTML pages at [http://localhost:9090](http://localhost:9090):

| Page       | URL                | Purpose                                                                         |
|------------|--------------------|---------------------------------------------------------------------------------|
| Overview   | `/`                | MaskMe library description, features, code comparison (hardcoded vs annotation) |
| Benchmarks | `/benchmarks.html` | Live benchmark runner, reference results, methodology, industry comparison      |
| API Demo   | `/api-demo.html`   | Interactive endpoint testing with visual user cards, side-by-side comparison    |

### API Demo Features

- **Side-by-side comparison** — fetches unmasked and masked user in one click, displayed side by side
- **Visual user cards** — masked fields highlighted in amber 🎭, excluded fields in green 🛡️
- **All 4 endpoints** — configurable headers and user selection
- **Raw JSON toggle** — expand to see the actual JSON response

## REST API Endpoints

### User Endpoints

| Method | URL                  | Headers                                         | Description                              |
|--------|----------------------|-------------------------------------------------|------------------------------------------|
| GET    | `/users/{id}`        | —                                               | Unmasked user DTO                        |
| GET    | `/users/masked/{id}` | `Mask-Input: maskMe`                            | Masked user (MaskMeOnInput condition)    |
| GET    | `/users/user/{id}`   | —                                               | Domain entity with AlwaysMaskMeCondition |
| GET    | `/users`             | `Mask-Input: maskMe`, `Mask-Phone: 01000000000` | All users with multiple conditions       |

### Benchmark Endpoint

| Method | URL          | Params                                                  | Description                       |
|--------|--------------|---------------------------------------------------------|-----------------------------------|
| GET    | `/benchmark` | `warmup` (default: 5000), `iterations` (default: 50000) | Runs live benchmark, returns JSON |

**Example:**

```bash
# Unmasked
curl http://localhost:9090/users/1

# Masked (MaskMeOnInput)
curl -H "Mask-Input: maskMe" http://localhost:9090/users/masked/1

# Multiple conditions
curl -H "Mask-Input: maskMe" -H "Mask-Phone: 01000000000" http://localhost:9090/users

# Live benchmark
curl "http://localhost:9090/benchmark?warmup=5000&iterations=50000"
```

## Benchmark

### What It Compares

| Approach      | How It Masks                                          | Code                            |
|---------------|-------------------------------------------------------|---------------------------------|
| **Hardcoded** | Manual `new UserDto(...)` with inline masking logic   | `hardcoded/dto/UserDto.mask()`  |
| **MaskMe**    | `MaskMeInitializer.mask(dto, Condition.class, input)` | Annotation-driven via `@MaskMe` |

Both approaches mask the same 15 fields (11 UserDto + 4 AddressDto) with the same masking rules.

### Scenarios

| Scenario            | Description                               |
|---------------------|-------------------------------------------|
| Single Condition    | `MaskMeOnInput` only                      |
| Multiple Conditions | `MaskMeOnInput` + `PhoneMaskingCondition` |
| Batch Processing    | 1,000 sequential masking operations       |
| Concurrent          | 10 threads × 1,000 operations each        |

## Benchmark Methodology

The benchmark follows **JMH-style methodology** (7 of 8 best practices):

```
Warmup (10K iterations)
    → JIT compilation & C2 optimization
    ↓
GC Stabilization
    → System.gc() + 100ms pause
    ↓
Measurement (3 runs × 100K iterations)
    → Per-sample System.nanoTime() timing
    → Memory via Runtime.totalMemory() - freeMemory()
    → CPU via ThreadMXBean.getCurrentThreadCpuTime()
    → GC via GarbageCollectorMXBean.getCollectionCount()
    ↓
Multi-Run Averaging
    → 3 runs averaged to reduce OS/thermal variance
    ↓
Statistical Analysis
    → Sort samples → p95, p99 percentiles
    → Throughput = ops / elapsed time
```

### What We Follow

| Practice                  | Implementation                                               |
|---------------------------|--------------------------------------------------------------|
| Warmup phase              | 10,000 iterations for JIT optimization                       |
| GC stabilization          | `System.gc()` + sleep between warmup and measurement         |
| Per-sample timing         | `System.nanoTime()` per iteration in pre-allocated ArrayList |
| Percentile analysis       | Sorted samples, p95/p99 by index                             |
| Multi-dimensional metrics | Time, memory, CPU, GC, throughput                            |
| High iteration count      | 100,000 per run                                              |
| Same data/conditions      | Identical DTO, same field count, same values                 |
| Multi-run averaging       | 3 runs averaged (±2-5% variance)                             |

### Honest Limitations

| Limitation           | Impact                                      | Mitigation                                                   |
|----------------------|---------------------------------------------|--------------------------------------------------------------|
| No fork isolation    | JIT profile pollution between benchmarks    | Hardcoded runs first → any bias favors MaskMe (conservative) |
| No Blackhole         | Theoretical dead-code elimination           | Both paths have side effects (object creation, reflection)   |
| Timer overhead       | ~20-30ns per sample (~3% on hardcoded path) | Both pay same cost → relative comparison valid               |
| Memory approximation | Snapshot, not precise allocation            | Same method for both → relative comparison valid             |

## Benchmark Results

**Environment:** aarch64, JVM 21.0.9, 6144MB RAM, 3 runs × 100K iterations

### Single Condition

| Metric              | Hardcoded | MaskMe   | Impact  |
|---------------------|-----------|----------|---------|
| **Avg Time**        | 0.0008ms  | 0.0806ms | +9,767% |
| **95th Percentile** | 0.0016ms  | 0.0917ms | +5,544% |
| **Memory per Op**   | 0.73 KB   | 0.30 KB  | -59%    |
| **CPU Usage**       | 99.1%     | 98.9%    | ~0%     |
| **GC Collections**  | 4         | 70       | +1,650% |

### Multiple Conditions

| Metric              | Hardcoded | MaskMe   | Impact   |
|---------------------|-----------|----------|----------|
| **Avg Time**        | 0.0004ms  | 0.0812ms | +18,891% |
| **95th Percentile** | 0.0008ms  | 0.0903ms | +11,944% |
| **Memory per Op**   | 0.51 KB   | 0.26 KB  | -49%     |
| **CPU Usage**       | 95.0%     | 98.9%    | +4%      |
| **GC Collections**  | 4         | 71       | +1,675%  |

### Batch & Concurrent

| Scenario                | Hardcoded        | MaskMe       | Impact   |
|-------------------------|------------------|--------------|----------|
| Batch (1000) — Avg Time | 0.0004ms         | 0.0815ms     | +19,590% |
| Batch — Throughput      | 2,415,704 ops/s  | 12,269 ops/s | -99.5%   |
| Concurrent (10 threads) | 1ms              | 388ms        | +38,700% |
| Concurrent — Throughput | 10,000,000 ops/s | 25,773 ops/s | -99.7%   |

## Real-World Context

> ⚠️ **Don't let the microbenchmark scare you.** The +9,767% compares two near-zero values in isolation.

```
Typical web request:

Total Response Time: 100ms
├─ Network latency:    20ms    (20%)
├─ Database query:     40ms    (40%)
├─ Business logic:     15ms    (15%)
├─ Authentication:     10ms    (10%)
├─ JSON serialization:  5ms    (5%)    ← Jackson
└─ MaskMe masking:    0.08ms   (0.08%) ← 11 fields + nested
```

| Perspective                         | Hardcoded  | MaskMe   | Impact                                        |
|-------------------------------------|------------|----------|-----------------------------------------------|
| **Microbenchmark** (isolated)       | 0.0008ms   | 0.08ms   | +9,767%                                       |
| **Real application** (full request) | 99.92ms    | 100ms    | **+0.08%**                                    |
| **Per-field cost**                  | ~0.00007ms | ~0.007ms | Competitive with Jackson (~0.01-0.05ms/field) |

### When to Use Which

| Scenario                        | Recommendation |
|---------------------------------|----------------|
| Ultra-high perf (<10ms budget)  | Hardcoded      |
| Simple masking (1-3 fields)     | Hardcoded      |
| >10K req/sec hot paths          | Hardcoded      |
| Complex conditional masking     | **MaskMe**     |
| Multiple DTOs / endpoints       | **MaskMe**     |
| Team collaboration              | **MaskMe**     |
| Compliance / audit trail        | **MaskMe**     |
| New development / microservices | **MaskMe**     |

## Tech Stack

| Component | Technology                                          |
|-----------|-----------------------------------------------------|
| Framework | Spring Boot 4.0.1                                   |
| Java      | 21                                                  |
| Masking   | [MaskMe 1.0.0](https://github.com/JAVA-MSDT/MaskMe) |
| Mapping   | MapStruct 1.4.2                                     |
| Lombok    | 1.18.30                                             |
| Build     | Maven                                               |
| Frontend  | Vanilla HTML/CSS/JS (no framework)                  |

---

**Author:** [Ahmed Samy](https://github.com/JAVA-MSDT) | [LinkedIn](https://www.linkedin.com/in/java-msdt/)
